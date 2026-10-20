# Especificação técnica

## Contexto

- Brief: `sdd/incrementos/painel-clima/brief.md`
- PRD: `.compozy/tasks/painel-clima/_prd.md`
- Contratos vivos: nenhum — `sdd/contratos/` não existe (PRM-002). Único comportamento existente a
  preservar: `GET /healthcheck` → `200 text/plain "OK"` (`HealthCheckController`).
- Impactos previstos: domínio `clima` **NOVO** (dois recursos HTTP internos `GET /cities` e
  `GET /weather`, painel na página inicial, estados de UI, mensagens e formato de erro). Nenhum
  `ALTERADO`/`REMOVIDO` em contrato. O conteúdo de `impacto-contratual/clima/contrato.md` é escrito no
  passo 03 a partir das seções "Contratos entre camadas" e "Frontend" desta TechSpec.
- Fonte de valores concretos: esta TechSpec é a fonte única dos nomes, caminhos, chaves, tempos-limite,
  códigos, mensagens e tabelas exigidos por `prompt_1.md` (seção de lacunas reservadas à TechSpec) e pelas
  skills de `.claude/skills/` (PRM-010). O mapa lacuna → seção está no fim do documento.

## Resumo da abordagem

- **Backend** expõe dois recursos de leitura, sem estado e idempotentes (ADR-001):
  - `GET /cities?name=` resolve um nome em até 10 cidades (Open-Meteo Geocoding);
  - `GET /weather?latitude=&longitude=` devolve as condições atuais de uma coordenada (Open-Meteo
    Forecast), já traduzidas para PT-BR (condição WMO e direção do vento em 8 pontos).
- **Frontend** orquestra o fluxo em um único componente (`WeatherPanel`): busca → (0 → não encontrada |
  1 → clima direto | 2–10 → lista) → clima da cidade escolhida; e geolocalização → clima de
  "Sua localização". Um `Subject` + `switchMap` serializa as operações (a mais recente cancela as
  anteriores); cada operação inteira tem teto de 15 s (`timeout`) e é a unidade de "Tentar novamente"
  (ADR-002).
- Sem retry automático em nenhuma camada; sem cache; sem persistência. Erros saem do backend sempre como
  `ErrorResponse(code, message)` e o frontend traduz pelo `code` para a sua própria tabela de mensagens.
- SSR (prerender): a página é renderizada no servidor no estado "Inicial" com tema `day`, sem HTTP e sem
  APIs do navegador; disponibilidade de geolocalização é detectada em `afterNextRender`.

## Código existente explorado

- Módulos/pacotes:
  - backend: `com.cezar.dev.airquality` com `AirqualityApplication` e
    `controllers/HealthCheckController` (`@GetMapping("/healthcheck")` → `"OK"`); teste único
    `AirqualityApplicationTests.contextLoads`; `application.properties` só com `spring.application.name`.
  - frontend: `App` (`app-root`) com placeholder do Angular e `RouterOutlet`; `app.routes.ts` vazio;
    `app.routes.server.ts` com `RenderMode.Prerender` para `**`; `app.config.ts` com
    `provideBrowserGlobalErrorListeners`, `provideRouter`, `provideClientHydration(withEventReplay())`
    e **sem** `provideHttpClient`; `index.html` com `lang="en"`, título `AirQuality` e sem fonte;
    `styles.css` só com `@import 'tailwindcss'`; `app.spec.ts` espera `h1` "Hello, air-quality".
- Versões efetivas: Spring Boot 4.0.6 / Java 25; Jackson 3 (`tools.jackson.core:jackson-databind` 3.x)
  com anotações em `com.fasterxml.jackson.annotation` (jackson-annotations 2.20/2.21); Angular 21.2.13;
  Vitest 4; RxJS 7.8.
- Pacotes de teste do Boot 4 (conferidos nos jars em `~/.m2`):
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`,
  `org.springframework.boot.restclient.test.autoconfigure.RestClientTest`;
  `@MockitoBean` em `org.springframework.test.context.bean.override.mockito`.
- Padrões existentes: injeção por construtor, controllers finos, nomes Angular sem sufixo
  (`app.ts` → `App`), Prettier `printWidth: 100` com aspas simples.
- Comandos de qualidade: `cd backend && ./mvnw test`; `cd frontend && npm test`;
  `cd frontend && npm run build`; varredura da skill `validar-entrega`.
- Restrições encontradas:
  - `compose.yaml`, `angular.json`/`baseHref`, `server.ts`, `main.server.ts`, `app.config.server.ts`,
    `app.routes.server.ts`, `pom.xml` e `package.json` (dependências) intocáveis;
  - prerender em build: o construtor do orquestrador roda no servidor, então nada nele pode fazer HTTP
    nem tocar `navigator`;
  - o `compose.yaml` reescreve o `baseHref` no bind mount ao subir (diferença esperada, PRM-009);
  - verificação manual via `compose.yaml` em `https://test.cezar.dev/<VERSION>/` (PRM-008).
- Comportamento real da Open-Meteo, sondado em 2026-10-20 (somente exploração; testes nunca acessam rede):
  - Geocoding sem resultado devolve `200 {"generationtime_ms":…}` **sem** a chave `results`;
  - Geocoding com `language=pt` devolve `name`, `admin1`, `admin2`, `country` localizados; "São José"
    devolve **duas** cidades "São José — Santa Catarina, Brasil" (diferem só por `admin2`:
    "São José" e "Braço do Norte") → motiva o campo `subregion` (PRM-020);
  - Forecast com `timezone=auto` devolve `current.time` em hora **local sem offset**
    (`"2026-10-20T16:00"`), `timezone` (`"America/Recife"`), `utc_offset_seconds` e `is_day` como `0/1`;
  - Forecast com parâmetro inválido devolve `400 {"error":true,"reason":"…"}`;
  - a Geocoding faz **casamento por prefixo** (≥ 3 caracteres) e devolve também aeroportos, ilhas e
    localidades com nome alternativo: "Recife" → 7 resultados (4 "Recife" PPL/PPLA/PPLL, 1 ilha `ISL`,
    1 aeroporto `AIRP`, 1 "Itapixuna"); "Curitiba" → 9 (inclui "Curitibanos" e "Curitiba Airport");
    "Florianópolis" → 7; "São Paulo" → 10. Nomes com resultado único são raros
    ("Pindamonhangaba" → 1; "Ouagadougou" → 1 PPLC + 1 AIRP). Motiva o filtro por `feature_code` e a
    PRM-021.

## Componentes e responsabilidades

Backend (`backend/src/main/java/com/cezar/dev/airquality/`):

- **`controllers/CityController`** — `GET /cities`; recebe `name` como `String` opcional e delega a
  `CityService.search`.
- **`controllers/WeatherController`** — `GET /weather`; recebe `latitude`/`longitude` como `String`
  opcionais e delega a `WeatherService.current`.
- **`controllers/ApiExceptionHandler`** — único `@RestControllerAdvice`; traduz as exceções próprias para
  `ResponseEntity<ErrorResponse>` com o status do contrato.
- **`services/CityService`** — normaliza/valida o nome (BR-001), chama `GeocodingApiClient`, trata lista
  vazia (→ `CityNotFoundException`), limita a 10 e mapeia para `CityResponse`.
- **`services/WeatherService`** — converte/valida coordenadas, chama `ForecastApiClient`, valida o bloco
  `current` e mapeia para `WeatherResponse`; contém as traduções puras `describeWeatherCode(Integer)` e
  `windDirection(Double)` (package-private `static`).
- **`clients/OpenMeteoConfig`** — `@Configuration` com os beans `geocodingApiRestClient` e
  `forecastApiRestClient`, cada um com `SimpleClientHttpRequestFactory` e seus timeouts.
- **`clients/GeocodingApiClient`** — única classe que faz HTTP para a Geocoding API.
- **`clients/ForecastApiClient`** — única classe que faz HTTP para a Forecast API.
- **`dtos/`** — `CityResponse`, `WeatherResponse`, `ErrorResponse` (respostas) e `GeocodingApiResponse`,
  `ForecastApiResponse` (JSON externo, com records aninhados).
- **`exceptions/`** — `InvalidRequestException(code, message)`, `CityNotFoundException`,
  `UpstreamException`, `UpstreamTimeoutException` (todas `RuntimeException`).

Frontend (`frontend/src/app/weather/`, feature `weather`):

- **`weather-api.ts` → `WeatherApi`** — único ponto de HTTP; `searchCities(name)` e
  `getWeather(latitude, longitude)`; exporta as interfaces `City`, `Weather`, `ApiError`.
- **`browser-location.ts` → `BrowserLocation`** (`providedIn: 'root'`) — embrulha
  `navigator.geolocation`: `isAvailable()` e `getPosition()` como `Observable`; nunca toca `navigator`
  no servidor.
- **`city-name.ts`** — funções puras `normalizeCityName(raw)` e `isValidCityName(name)` (BR-001) e as
  constantes `CITY_NAME_MIN = 2`, `CITY_NAME_MAX = 100`.
- **`weather-messages.ts`** — tabela única de textos da UI (constante `MESSAGES`) e a função pura
  `toFailure(error)` que mapeia erro → estado/mensagem/retry.
- **`weather-panel.ts` → `WeatherPanel` (`app-weather-panel`)** — orquestrador: estado, operações,
  cancelamento, teto de 15 s, retry, tema, foco.
- **`city-search-form.ts` → `CitySearchForm` (`app-city-search-form`)** — apresentação: campo, botão
  "Buscar", botão "Usar minha localização", mensagem de validação.
- **`city-options.ts` → `CityOptions` (`app-city-options`)** — apresentação: lista de homônimas.
- **`weather-card.ts` → `WeatherCard` (`app-weather-card`)** — apresentação: condições atuais.
- **`weather-skeleton.ts` → `WeatherSkeleton` (`app-weather-skeleton`)** — apresentação: carregamento com
  a mesma grade do cartão.
- **`status-message.ts` → `StatusMessage` (`app-status-message`)** — apresentação: instrução inicial,
  "não encontrada", falhas e mensagens de localização, com "Tentar novamente" opcional.
- **`App` (`app.ts`/`app.html`)** — passa a renderizar só `<app-weather-panel />`.

## Fluxo de dados

```text
[Busca por nome]
CitySearchForm (submit) ─raw─▶ WeatherPanel
  normalizeCityName + isValidCityName ──inválido──▶ estado invalid (sem HTTP, foco no input)
  válido ─▶ operação "search(name)" no Subject ─switchMap─▶
    WeatherApi.searchCities ─GET api/cities?name=…─▶ Traefik (remove /<VERSION>/api)
      ─▶ CityController ─▶ CityService (strip, 2..100) ─▶ GeocodingApiClient ─▶ Open-Meteo Geocoding
      ◀─ List<CityResponse> (1..10) | 404 CITY_NOT_FOUND | 400 | 502 | 504
    0 cidades (404) ─▶ estado not-found
    1 cidade ─▶ (mesmo observable interno) WeatherApi.getWeather(lat, lon) ─▶ estado success
    2..10 ─▶ estado choosing (lista, foco no 1º item)

[Escolha na lista]  CityOptions (selected) ─▶ operação "select(city)" ─▶ getWeather ─▶ success

[Localização]  CitySearchForm (useLocation) ─▶ operação "locate()" ─▶
    BrowserLocation.getPosition (≤ 8 s) ─▶ arredonda para 2 casas ─▶ getWeather ─▶ success
    ("Sua localização") | geo-denied | geo-unavailable | unavailable | timeout

Toda operação: estado loading → … ; `timeout(15000)` sobre a cadeia inteira; resultado de operação
antiga nunca chega ao estado (switchMap); falha repetível guarda a operação para "Tentar novamente".
```

Persistência: nenhuma. Integração: duas chamadas HTTP de saída, sempre no backend.

## Backend

### Endpoints

| Verbo  | Caminho (Spring) | URL no navegador     | Parâmetros (query, recebidos como `String`, `required = false`) | Sucesso |
|--------|------------------|----------------------|-------------------------------------------------------------------|---------|
| GET    | `/cities`        | `api/cities`         | `name`                                                            | `200` `CityResponse[]` (1 a 10 itens) |
| GET    | `/weather`       | `api/weather`        | `latitude`, `longitude`                                           | `200` `WeatherResponse` |
| GET    | `/healthcheck`   | `api/healthcheck`    | —                                                                 | `200` `"OK"` (inalterado) |

Receber tudo como `String` opcional faz parâmetro ausente, vazio ou não numérico cair sempre nas
exceções próprias com o `code` do endpoint; o `ApiExceptionHandler` **não** precisa tratar
`MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException`.

### Validação (no service, antes de qualquer HTTP externo)

- **`name`** (BR-001, RF-002):
  1. `null` → inválido;
  2. normalizar removendo das extremidades o **conjunto explícito** Unicode `White_Space` + U+FEFF,
     igual nos dois lados (nem `String.strip()` nem `trim()`: `strip()` usa `Character.isWhitespace`,
     que **não** remove U+00A0/U+2007/U+202F, e o `trim()` do JS remove U+00A0 e U+FEFF):
     Java `name.replaceAll("^[\\p{IsWhite_Space}\\uFEFF]+|[\\p{IsWhite_Space}\\uFEFF]+$", "")`;
     TS `raw.replace(/^[\p{White_Space}\uFEFF]+|[\p{White_Space}\uFEFF]+$/gu, '')`;
  3. tamanho medido em **code points** (Java `name.codePointCount(0, name.length())`; TS
     `[...name].length`) entre **2 e 100** inclusive; fora disso →
     `InvalidRequestException("INVALID_CITY_NAME", …)`;
  4. nenhuma restrição de conjunto de caracteres (acentos, hífen, apóstrofo, espaços internos aceitos);
  5. o valor normalizado é o que vai para a Open-Meteo.
- **`latitude`/`longitude`**:
  1. `null`/vazio após a mesma normalização de extremidades → inválido;
  2. `Double.parseDouble` (falha → inválido); `NaN`/`Infinity` → inválido;
  3. latitude em `[-90, 90]`, longitude em `[-180, 180]` (inclusive);
  4. qualquer falha → `InvalidRequestException("INVALID_COORDINATES", …)`.

### Regras de mapeamento

- `CityService`:
  - mantém apenas localidades habitadas: `feature_code` que começa com `"PPL"` (PPL, PPLA, PPLA2…,
    PPLC, PPLL, PPLX…) ou `feature_code` ausente; descarta `AIRP`, `ISL` etc. (decisão técnica desta
    TechSpec: "cidade candidata" de BR-002 = localidade habitada);
  - `results` ausente (`null`), vazio ou vazio após o filtro → `CityNotFoundException` (404);
  - mantém a ordem do provedor e corta em 10 (`count=10` já é pedido; o corte é defensivo);
  - item sem `id`, `name`, `latitude` ou `longitude` → `UpstreamException` (502) (corpo incompleto);
  - `admin1` → `region`, `admin2` → `subregion`, `country` → `country`, `country_code` → `countryCode`,
    `timezone` → `timezone`; ausentes viram `null` (nunca omitidos).
- `WeatherService`:
  - `current` ausente ou `current.time` ausente/vazio → `UpstreamException` (502);
  - demais campos de `current` ausentes → `null` no DTO (BR-007/RF-003 "—");
  - `isDay`: `1` → `true`, `0` → `false`, outro/ausente → `null`;
  - `condition = describeWeatherCode(weatherCode)` (nunca `null`; código `null` ou desconhecido →
    "Condição desconhecida");
  - `windDirection = windDirection(windDirectionDegrees)` (`null` se graus `null`/não finito);
  - `latitude`/`longitude` da resposta = as **recebidas na requisição** (não as da grade do provedor),
    e `timezone` = `timezone` do provedor (`null` se ausente).

### Tabela de tradução — `weather_code` (WMO) → `condition`

| Código | `condition` (PT-BR)                  | Código | `condition` (PT-BR)                    |
|--------|--------------------------------------|--------|----------------------------------------|
| 0      | Céu limpo                            | 63     | Chuva moderada                         |
| 1      | Predominantemente limpo              | 65     | Chuva forte                            |
| 2      | Parcialmente nublado                 | 66     | Chuva congelante fraca                 |
| 3      | Nublado                              | 67     | Chuva congelante forte                 |
| 45     | Nevoeiro                             | 71     | Neve fraca                             |
| 48     | Nevoeiro com geada                   | 73     | Neve moderada                          |
| 51     | Garoa fraca                          | 75     | Neve forte                             |
| 53     | Garoa moderada                       | 77     | Grãos de neve                          |
| 55     | Garoa intensa                        | 80     | Pancadas de chuva fracas               |
| 56     | Garoa congelante fraca               | 81     | Pancadas de chuva moderadas            |
| 57     | Garoa congelante intensa             | 82     | Pancadas de chuva violentas            |
| 61     | Chuva fraca                          | 85     | Pancadas de neve fracas                |
| 95     | Trovoada                             | 86     | Pancadas de neve fortes                |
| 96     | Trovoada com granizo fraco           | 97     | Trovoada forte                         |
| 99     | Trovoada com granizo forte           | `null` ou qualquer outro (ex.: 4, 100, -1) | **Condição desconhecida** |

Implementação: `Map.ofEntries(...)` imutável + `getOrDefault`. Teste parametrizado cobre as 29 entradas
e os casos `null`, `4`, `100`, `-1`.

### Tabela de tradução — graus → `windDirection` (8 pontos)

Normalização: `d = ((graus % 360) + 360) % 360` (360° ≡ 0°; negativos e > 360° aceitos);
índice `= (int) Math.floor((d + 22.5) / 45) % 8`.

| Faixa de `d` (início inclusivo, fim exclusivo) | `windDirection` |
|------------------------------------------------|-----------------|
| `[337.5, 360)` ∪ `[0, 22.5)`                   | Norte           |
| `[22.5, 67.5)`                                 | Nordeste        |
| `[67.5, 112.5)`                                | Leste           |
| `[112.5, 157.5)`                               | Sudeste         |
| `[157.5, 202.5)`                               | Sul             |
| `[202.5, 247.5)`                               | Sudoeste        |
| `[247.5, 292.5)`                               | Oeste           |
| `[292.5, 337.5)`                               | Noroeste        |
| `null`, `NaN`, `±Infinity`                     | `null`          |

Casos de teste obrigatórios: 0, 22.49, 22.5, 45, 67.49, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5,
337.49, 337.5, 359.99, 360, 720, -45 (→ Noroeste), `null`, `NaN`.

### Erros (`ErrorResponse(code, message)`, via `ApiExceptionHandler`)

| Status | `code`                | Quando                                                        | `message` (PT-BR, exata) |
|--------|-----------------------|---------------------------------------------------------------|--------------------------|
| 400    | `INVALID_CITY_NAME`   | `name` ausente, vazio ou fora de 2–100 após a normalização    | Informe o nome de uma cidade com 2 a 100 caracteres. |
| 400    | `INVALID_COORDINATES` | `latitude`/`longitude` ausentes, não numéricas ou fora da faixa | Coordenadas inválidas. Use latitude entre -90 e 90 e longitude entre -180 e 180. |
| 404    | `CITY_NOT_FOUND`      | Geocoding sem resultados                                      | Nenhuma cidade encontrada com esse nome. |
| 502    | `UPSTREAM_ERROR`      | Open-Meteo com status ≠ 2xx (inclui o `400 {"error":true}`), corpo vazio/inválido/incompleto, conexão recusada/falha de I/O | O serviço de clima está indisponível no momento. Tente novamente em instantes. |
| 504    | `UPSTREAM_TIMEOUT`    | Timeout de conexão ou de leitura (`SocketTimeoutException`/`HttpTimeoutException` na cadeia de causas) | O serviço de clima demorou demais para responder. Tente novamente. |

- Sem `@ExceptionHandler(Exception.class)`: um handler genérico capturaria `NoResourceFoundException` e
  `HttpRequestMethodNotSupportedException`, trocando 404/405 por 500. Erros não previstos ficam com o
  `/error` padrão do Boot, que por padrão não inclui stack trace nem mensagem
  (`server.error.include-stacktrace=never`, `include-message=never`); nada nesta entrega altera isso.
- O `reason` da Open-Meteo e a mensagem de qualquer exceção Java nunca vão para o corpo.

### Clients e configuração

`application.properties` (valores exatos):

```properties
spring.application.name=airquality
geocoding-api.base-url=https://geocoding-api.open-meteo.com/v1
geocoding-api.connect-timeout=2s
geocoding-api.read-timeout=4s
forecast-api.base-url=https://api.open-meteo.com/v1
forecast-api.connect-timeout=2s
forecast-api.read-timeout=4s
```

- `OpenMeteoConfig` cria `RestClient geocodingApiRestClient(RestClient.Builder, @Value connect,
  @Value read)` e `RestClient forecastApiRestClient(...)`; cada bean monta sua própria
  `SimpleClientHttpRequestFactory` (`setConnectTimeout(Duration)`, `setReadTimeout(Duration)`).
  `RestClient.Builder` do Boot é prototype: cada bean recebe um builder novo.
- Os clients recebem o bean por `@Qualifier("geocodingApiRestClient")` /
  `@Qualifier("forecastApiRestClient")` e a base URL por `@Value("${…base-url}")`.
- `GeocodingApiClient.search(String name)` → `GET {base}/search?name={name}&count=10&language=pt&format=json`.
- `ForecastApiClient.current(double latitude, double longitude)` →
  `GET {base}/forecast?latitude=…&longitude=…&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m&timezone=auto&temperature_unit=celsius&wind_speed_unit=kmh`.
- URI sempre por `UriComponentsBuilder.fromUriString(baseUrl).path(...).queryParam(...).encode().build().toUri()`
  (acentos e espaços de "São José" codificados uma única vez).
- Tradução de falhas exatamente como na skill `cliente-api-externa`: `RestClientResponseException` →
  `UpstreamException`; `ResourceAccessException` → `UpstreamTimeoutException` se houver
  `SocketTimeoutException`/`HttpTimeoutException` na cadeia de causas, senão `UpstreamException`;
  `RestClientException` (desserialização) → `UpstreamException`; corpo `null` → `UpstreamException`.
  Com `SimpleClientHttpRequestFactory`, timeout de **conexão** também chega como
  `SocketTimeoutException("Connect timed out")` → 504.

### DTOs do JSON externo (Jackson 3)

- Records em `dtos/`, todos com `@JsonIgnoreProperties(ignoreUnknown = true)` e campos em tipos
  wrapper (`Long`, `Double`, `Integer`, `String`, `List`), anotações de
  `com.fasterxml.jackson.annotation` (`@JsonProperty`, `@JsonIgnoreProperties`). Não depender do
  default do mapper: os testes de client usam `RestClient.builder()` puro, sem o mapper do Boot.
- `GeocodingApiResponse(List<Result> results)` com
  `record Result(Long id, String name, Double latitude, Double longitude, String admin1, String admin2,
  String country, @JsonProperty("country_code") String countryCode,
  @JsonProperty("feature_code") String featureCode, String timezone)`.
- `ForecastApiResponse(String timezone, Current current)` com
  `record Current(String time, @JsonProperty("temperature_2m") Double temperature2m,
  @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
  @JsonProperty("apparent_temperature") Double apparentTemperature, @JsonProperty("is_day") Integer isDay,
  @JsonProperty("weather_code") Integer weatherCode, @JsonProperty("wind_speed_10m") Double windSpeed10m,
  @JsonProperty("wind_direction_10m") Double windDirection10m)`.

### Autorização e isolamento

Não há autenticação (fora de escopo). Endpoints só leem dados públicos; não há tenancy. Sem CORS: mesma
origem via Traefik.

## Frontend

### Arquivos

```text
frontend/src/index.html            lang="pt-BR", <title>Painel de clima</title>, <link> JetBrains Mono
frontend/src/styles.css            @import 'tailwindcss' + todos os tokens + base de html/body
frontend/src/app/app.config.ts     + provideHttpClient(withFetch())
frontend/src/app/app.ts|html|css   App renderiza <app-weather-panel />; remove RouterOutlet do template
frontend/src/app/app.spec.ts       passa a verificar a presença de app-weather-panel
frontend/src/app/weather/
  weather-api.ts (+ .spec.ts)              WeatherApi
  browser-location.ts (+ .spec.ts)         BrowserLocation
  city-name.ts (+ .spec.ts)                normalizeCityName, isValidCityName
  weather-messages.ts (+ .spec.ts)         MESSAGES, toFailure
  weather-panel.ts|html|css|spec.ts        WeatherPanel        app-weather-panel
  city-search-form.ts|html|css|spec.ts     CitySearchForm      app-city-search-form
  city-options.ts|html|css|spec.ts         CityOptions         app-city-options
  weather-card.ts|html|css|spec.ts         WeatherCard         app-weather-card
  weather-skeleton.ts|html|css|spec.ts     WeatherSkeleton     app-weather-skeleton
  status-message.ts|html|css|spec.ts       StatusMessage       app-status-message
```

`provideRouter(routes)` e `app.routes.ts` permanecem (rotas vazias). Não há rotas novas: o painel é a
página inicial.

### `index.html`

- `<html lang="pt-BR">`, `<title>Painel de clima</title>`.
- `<link rel="preconnect" href="https://fonts.googleapis.com">`,
  `<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>` e
  `<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&display=swap">`
  (único recurso externo permitido). `<base href="/">` não é tocado (o build reescreve).

### `styles.css`

- Mantém `@import 'tailwindcss';` como primeira regra; nenhuma classe utilitária nem `@theme`.
- `:root, [data-theme='day']` define os tokens de cor `day`; `[data-theme='night']` os de `night`
  (valores exatos de `design-system.md`). O `:root` garante fundo `day` fora do painel e no SSR.
- `:root` define `--radius-control: 4px`, `--radius-container: 0px`, `--font-mono`,
  `--space-4/8/12/16/24/32/48/96`, e os auxiliares de tamanho `--font-size-base: 16px`,
  `--font-size-small: 14px`, `--control-height: 40px`, e os valores que a própria `design-system.md`/
  `acessibilidade.md` prescrevem fora da escala de espaçamento, como tokens nomeados:
  `--button-padding-x: 20px` (botões `4px 20px`), `--focus-ring-width: 2px` e `--focus-ring-offset: 2px`
  (`outline`), `--border-width: 1px`, `--container-max: 640px`. Componentes usam só esses `var(--…)`.
- `html, body { margin: 0; background: var(--color-bg); color: var(--color-text);
  font-family: var(--font-mono); font-size: var(--font-size-base); line-height: 1.5; letter-spacing: 0; }`.
- Classe utilitária própria `.visually-hidden` (texto só para leitor de tela) — CSS do projeto, não
  Tailwind.

### Estado do orquestrador (`WeatherPanel`)

```ts
type PanelState =
  | 'idle' | 'loading' | 'success' | 'choosing' | 'invalid'
  | 'not-found' | 'unavailable' | 'timeout' | 'geo-denied' | 'geo-unavailable';
type Theme = 'day' | 'night';
type Operation =
  | { kind: 'search'; name: string }
  | { kind: 'select'; city: City }
  | { kind: 'locate' }
  | { kind: 'cancel' }; // emitida por submit inválido: só cancela a operação em curso (run → EMPTY)
```

Signals: `state`, `weather` (`Weather | null`), `locationLabel` (`string | null`), `cities`
(`City[]`), `theme` (`Theme`, inicial `'day'`), `geolocationAvailable` (`boolean`, inicial `false`),
`lastFailed` (`Operation | null`).
Computeds: `busy = state() === 'loading'`; `failure` (mensagem + `retryable` do estado atual).

Transições:

| Evento                                       | Próximo estado                                   | Efeitos |
|----------------------------------------------|--------------------------------------------------|---------|
| carga (SSR e navegador)                      | `idle`                                           | `theme='day'` |
| `afterNextRender`                            | (sem mudança)                                    | `geolocationAvailable = BrowserLocation.isAvailable()` |
| submit com nome inválido                     | `invalid`                                        | nenhuma requisição; cancela operação em curso; foco no input |
| submit válido / escolha / localização        | `loading`                                        | `Subject.next(op)`; limpa `lastFailed` |
| `searchCities` → 1 cidade + `getWeather` ok  | `success`                                        | `weather`, `locationLabel` = rótulo da cidade, `theme` de `isDay` |
| `searchCities` → 2..10                       | `choosing`                                       | `cities`; foco no 1º item |
| `getWeather` ok (escolha)                    | `success`                                        | idem; rótulo da cidade escolhida |
| `getWeather` ok (localização)                | `success`                                        | `locationLabel = 'Sua localização'` |
| HTTP 404 `CITY_NOT_FOUND`                    | `not-found`                                      | — |
| HTTP 400 `INVALID_CITY_NAME`                 | `invalid`                                        | foco no input (só ocorre se cliente e servidor divergirem) |
| HTTP 504 `UPSTREAM_TIMEOUT` ou `TimeoutError` (15 s) fora da espera da posição | `timeout`      | `lastFailed = op` |
| `TimeoutError` (15 s) em `locate` antes de obter a posição (ex.: permissão sem resposta) | `geo-unavailable` | `lastFailed = op` |
| HTTP 502, 5xx, `status 0`, qualquer outro erro não listado | `unavailable`                      | `lastFailed = op` |
| HTTP 400 `INVALID_COORDINATES` (bug, não transitório) | `unavailable`                            | `lastFailed = null` (sem retry, BR-004) |
| geolocalização `PERMISSION_DENIED` (code 1)  | `geo-denied`                                     | — (sem retry) |
| geolocalização `POSITION_UNAVAILABLE`(2)/`TIMEOUT`(3) | `geo-unavailable`                       | `lastFailed = op` |
| "Tentar novamente"                           | `loading`                                        | `Subject.next(lastFailed)` (mesma operação do usuário) |

- `theme` só muda em `success` (`isDay === false` → `'night'`; `true` ou `null` → `'day'`); em erros e
  na lista (`choosing`) permanece o do último sucesso. Conforme RF-010 e PRM-019
  (redação alinhada pelo dev em 2026-10-20, PRM-022), `day` vale só **antes do primeiro resultado**.
- `weather`, `cities` e `locationLabel` são zerados ao entrar em `loading`; a área de resultado mostra
  apenas o componente do estado atual (estados mutuamente exclusivos).
- Rótulo da cidade: `[name, region, country]` sem nulos, unido por `', '`.
- Mapeamento de erro em `toFailure(error)`: `TimeoutError` (rxjs) → `timeout`;
  `GeolocationPositionError`-like (`{ code: 1|2|3 }` vindo de `BrowserLocation`) → `geo-denied`/
  `geo-unavailable`; `HttpErrorResponse` por `error?.code` primeiro e `status` depois (tabela acima).
  O frontend **não** exibe `error.message` do backend: usa a sua tabela (`MESSAGES`).

### Operações, cancelamento e teto de tempo

```ts
this.operations.pipe(
  switchMap((op) => this.run(op).pipe(timeout(15_000), catchError((e) => this.fail(op, e)))),
  takeUntilDestroyed(),
).subscribe();
```

- `run(op)` devolve um único `Observable` por operação; o encadeamento busca → clima (1 cidade) e
  posição → clima ficam **dentro** dele (`switchMap` aninhado), para serem cancelados e cronometrados
  juntos.
- `timeout(15_000)` cobre a cadeia inteira, inclusive a espera da geolocalização (RNF-002).
- Submit inválido também cancela a operação em curso: emite `{ kind: 'cancel' }` no mesmo `Subject`
  (`run` devolve `EMPTY`), para que uma resposta antiga não sobrescreva o estado `invalid`.
- Fases do `locate`: o `timeout(15_000)` conta também o tempo do **pedido de permissão** do navegador
  (o `timeout: 8000` do `getCurrentPosition` não conta — W3C Geolocation). Por isso o `catchError`
  distingue a fase: `TimeoutError` **antes** de a posição ser emitida → `geo-unavailable` (com retry,
  RF-009); `TimeoutError` depois (fase do clima) → `timeout`. Implementação: flag local
  `positionReceived` setada num `tap` após `getPosition()`, lida por `fail(op, e)`.
- Cancelar o `Observable` de HTTP aborta a requisição (`withFetch` usa `AbortController`).
- Coordenadas da geolocalização arredondadas para 2 casas (`Math.round(v * 100) / 100`, ~1,1 km)
  antes de chamar `getWeather` (BR-010, minimização); nunca exibidas, nunca guardadas fora do escopo da
  operação, nunca em `console`.

### `BrowserLocation`

- Campo `private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));` (injeção no
  inicializador de campo — `inject()` chamado fora do contexto de injeção lança NG0203).
- `isAvailable()`: `this.isBrowser && 'geolocation' in navigator`.
- `getPosition()`: `new Observable<{ latitude: number; longitude: number }>` que chama
  `navigator.geolocation.getCurrentPosition(ok, err, { enableHighAccuracy: false, timeout: 8000,
  maximumAge: 300000 })`; `ok` emite e completa; `err` emite erro `{ code: err.code }`; no teardown,
  marca a assinatura como encerrada para ignorar callbacks tardios (a API não é cancelável).
- Só é chamado a partir de ação do usuário (BR-008); nunca no construtor nem em `afterNextRender`.

### Componentes de apresentação (contratos)

| Componente        | `input()`                                                                 | `output()`               | Notas de UI/a11y |
|-------------------|---------------------------------------------------------------------------|--------------------------|------------------|
| `CitySearchForm`  | `geolocationAvailable: boolean`, `busy: boolean`, `invalidMessage: string \| null` | `search: string` (texto bruto), `useLocation: void` | `<form>` com `<label for="city-name">Cidade</label>`, `<input id="city-name" type="text" autocomplete="address-level2">` (sem `maxlength`: ele conta unidades UTF-16, não code points, e divergiria de BR-001; o limite é validado no submit), `<button type="submit">Buscar</button>` (primário), `<button type="button">Usar minha localização</button>` (secundário, só se `geolocationAvailable`); mensagem de validação em `<p id="city-name-error" role="alert">` ligada por `aria-describedby` e `aria-invalid="true"`; função pública `focusInput()`; botões não ficam desabilitados durante `busy` (nova busca substitui a anterior, RF-007). Linha com gap 8px; empilha abaixo de 640px. |
| `CityOptions`     | `cities: City[]` (required)                                               | `selected: City`         | Título "Encontramos N cidades. Escolha uma:"; `<ul>` de `<li><button type="button">` (linha 40px, hairline, acento em hover/foco); texto `Nome — Região, País`; acrescenta ` (Subregião)` quando houver outro item com mesmo nome+região+país e `subregion` não nula (PRM-020); `track city.id`; foca o 1º botão após renderizar (`viewChildren` + `afterNextRender`). |
| `WeatherCard`     | `weather: Weather` (required), `locationLabel: string` (required)         | —                        | Título com o rótulo e badge `[ DIA ]`/`[ NOITE ]` (omitida se `isDay` nulo); linhas: temperatura (700), condição, "Sensação", "Umidade", "Vento" (`12,7 km/h · Leste`), "Medido às HH:mm (horário local)". Formatação em `computed`. |
| `WeatherSkeleton` | —                                                                         | —                        | Mesma grade/padding/`min-height` do `WeatherCard`; blocos `--color-surface-card` com pulso `1 → 0.5`, 1.2s, infinito; sem animação em `prefers-reduced-motion: reduce`; `aria-hidden="true"` + texto `.visually-hidden` "Carregando o clima…". |
| `StatusMessage`   | `message: string` (required), `kind: 'info' \| 'error'` (required), `retryable: boolean` (default `false`) | `retry: void` | `error`: `role="alert"`, marcador `[x]` em `--color-danger` (`aria-hidden`), texto em `--color-text`; `info`: marcador `[i]` em `--color-text-mute`, sem `role="alert"`; botão secundário "Tentar novamente" quando `retryable`. |

`WeatherPanel` template: elemento raiz `<main class="panel">` com `[attr.data-theme]="theme()"`,
`min-height: 100dvh`, `background: var(--color-bg)`; container 640px; título `<h1>Painel de clima</h1>`;
`<app-city-search-form>`; área `<section aria-live="polite" [attr.aria-busy]="busy()">` com `@switch`
sobre `state()`. O foco programático (input em `invalid`, 1º item em `choosing`) só roda no navegador.

### Formatação (no `computed`, sem `Date`)

- Números: `Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })` → `28,6`. Temperatura
  `28,6 °C`; umidade `62 %`; vento `12,7 km/h`. Campo `null` → `—` (direção `null` → só a velocidade;
  ambos `null` → `—`).
- Horário: `observedAt.slice(11, 16)` (`"2026-10-20T16:00"` → `16:00`). **Nunca** `new Date(observedAt)`:
  a string não tem offset e seria interpretada no fuso do navegador (BR-012).

### Mensagens da UI (`MESSAGES`, textos exatos)

| Estado            | Tipo   | Texto                                                                                   | Retry |
|-------------------|--------|-----------------------------------------------------------------------------------------|-------|
| `idle`            | info   | Digite o nome de uma cidade e pressione Enter para ver o clima atual.                  | não |
| `loading`         | —      | (skeleton; texto oculto) Carregando o clima…                                            | — |
| `invalid`         | error  | Informe o nome de uma cidade com 2 a 100 caracteres. (junto ao campo)                    | não |
| `not-found`       | error  | Nenhuma cidade encontrada com esse nome. Confira a grafia e busque novamente.            | não |
| `unavailable`     | error  | O serviço de clima está indisponível no momento.                                        | sim (exceto após `INVALID_COORDINATES`) |
| `timeout`         | error  | O serviço de clima demorou demais para responder.                                       | sim |
| `geo-denied`      | info   | Permissão de localização negada. Busque pelo nome da cidade.                            | não |
| `geo-unavailable` | info   | Não foi possível obter sua localização. Tente novamente ou busque pelo nome da cidade.  | sim |
| `choosing`        | —      | Encontramos {N} cidades. Escolha uma:                                                   | — |
| `success`         | —      | Rótulo do local; geolocalização: "Sua localização"                                      | — |

Botão de retry: "Tentar novamente". Em `invalid`, a área de resultado fica vazia e a mensagem aparece no
formulário.

### SSR

- Prerender: o construtor do `WeatherPanel` só monta o pipeline (sem emissão); nenhuma chamada a
  `WeatherApi` nem a `BrowserLocation.getPosition` fora de handlers de evento.
- `geolocationAvailable` começa `false` no servidor; o botão aparece depois da hidratação.
- `withEventReplay()` preserva cliques feitos antes da hidratação.

## Contratos entre camadas

Versão: v1 implícita, sem prefixo de versão (recurso interno, PRM-003). Mudanças futuras só aditivas
(campo novo nullable); remoção/renomeação exige novo incremento com impacto `ALTERADO`.

### `GET /cities?name={name}` → `200 application/json`

```json
[
  {
    "id": 3448744,
    "name": "São José",
    "region": "Santa Catarina",
    "subregion": "São José",
    "country": "Brasil",
    "countryCode": "BR",
    "latitude": -27.61528,
    "longitude": -48.6275,
    "timezone": "America/Sao_Paulo"
  }
]
```

| Campo         | Tipo JSON | Nulo | Origem Open-Meteo |
|---------------|-----------|------|-------------------|
| `id`          | number (long) | não | `id` |
| `name`        | string    | não  | `name` |
| `region`      | string    | sim  | `admin1` |
| `subregion`   | string    | sim  | `admin2` (PRM-020) |
| `country`     | string    | sim  | `country` |
| `countryCode` | string    | sim  | `country_code` |
| `latitude`    | number    | não  | `latitude` |
| `longitude`   | number    | não  | `longitude` |
| `timezone`    | string    | sim  | `timezone` |

Array com 1 a 10 itens, na ordem do provedor. Nenhuma correspondência → `404 CITY_NOT_FOUND` (nunca `[]`).

### `GET /weather?latitude={lat}&longitude={lon}` → `200 application/json`

```json
{
  "latitude": -8.05,
  "longitude": -34.88,
  "timezone": "America/Recife",
  "observedAt": "2026-10-20T16:00",
  "temperature": 28.6,
  "apparentTemperature": 30.8,
  "humidity": 62,
  "windSpeed": 12.7,
  "windDirectionDegrees": 111.0,
  "windDirection": "Leste",
  "weatherCode": 1,
  "condition": "Predominantemente limpo",
  "isDay": true
}
```

| Campo                  | Tipo JSON | Nulo | Unidade / regra |
|------------------------|-----------|------|-----------------|
| `latitude`/`longitude` | number    | não  | ecoam a requisição |
| `timezone`             | string    | sim  | IANA, do provedor |
| `observedAt`           | string    | não  | `yyyy-MM-ddTHH:mm`, hora local do lugar, **sem offset** |
| `temperature`          | number    | sim  | °C |
| `apparentTemperature`  | number    | sim  | °C |
| `humidity`             | number (int) | sim | % |
| `windSpeed`            | number    | sim  | km/h |
| `windDirectionDegrees` | number    | sim  | graus como recebidos |
| `windDirection`        | string    | sim  | tabela de 8 pontos |
| `weatherCode`          | number (int) | sim | WMO |
| `condition`            | string    | não  | tabela WMO; padrão "Condição desconhecida" |
| `isDay`                | boolean   | sim  | `is_day` 1/0 |

DTO Java: `record WeatherResponse(double latitude, double longitude, String timezone, String observedAt,
Double temperature, Double apparentTemperature, Integer humidity, Double windSpeed,
Double windDirectionDegrees, String windDirection, Integer weatherCode, String condition, Boolean isDay)`;
`record CityResponse(long id, String name, String region, String subregion, String country,
String countryCode, double latitude, double longitude, String timezone)`. Campos nulos serializados como
`null` (não omitidos — default do Jackson; não ativar `NON_NULL`).

### Erros

`application/json` `{"code": "...", "message": "..."}` — tabela na seção Backend. Tipos TS:

```ts
export interface City { id: number; name: string; region: string | null; subregion: string | null;
  country: string | null; countryCode: string | null; latitude: number; longitude: number;
  timezone: string | null; }
export interface Weather { latitude: number; longitude: number; timezone: string | null;
  observedAt: string; temperature: number | null; apparentTemperature: number | null;
  humidity: number | null; windSpeed: number | null; windDirectionDegrees: number | null;
  windDirection: string | null; weatherCode: number | null; condition: string; isDay: boolean | null; }
export interface ApiError { code: string; message: string; }
```

`WeatherApi`: `http.get<City[]>('api/cities', { params: { name } })` e
`http.get<Weather>('api/weather', { params: { latitude, longitude } })` — URLs relativas sem barra
inicial, parâmetros via `params`, coordenadas como `number` (o `HttpParams` serializa com `toString`).

## Dados e migração

Não há entidades, banco, cache ou armazenamento no navegador. Rollout: entrega única. Rollback: reverter
o commit/merge do incremento (arquivos novos + `app.*`, `index.html`, `styles.css`, `app.config.ts`,
`application.properties`); `GET /healthcheck` não depende de nada novo.

## Integrações

- **Open-Meteo Geocoding** (`geocoding-api.base-url`) — sem auth/chave (PRM-005); timeout de conexão 2 s
  e de leitura 4 s; sem retry (o usuário tem "Tentar novamente"); GET idempotente; sem circuit breaker
  (volume baixo, sem dependência nova — Resilience4j proibido); mocks com `MockRestServiceServer`.
- **Open-Meteo Forecast** (`forecast-api.base-url`) — idem, mesmos tempos.
- **Orçamento de tempo (RNF-002, ADR-002)** — `read-timeout` limita cada leitura do socket, não a duração
  total; com respostas pequenas da Open-Meteo, o pior caso prático por chamada é ≈ connect 2 s + read
  4 s = 6 s.
  - busca com 1 cidade: 6 s (geocoding) + 6 s (forecast) = 12 s < 15 s;
  - escolha na lista: 6 s;
  - localização: 8 s de aquisição da posição + 6 s = 14 s < 15 s **depois** da permissão concedida; o
    `timeout: 8000` do navegador não conta o tempo do pedido de permissão, mas o teto de 15 s conta;
    estourar antes de ter a posição → `geo-unavailable`, depois → `timeout`;
  - em qualquer caso o `timeout(15_000)` do frontend é o teto absoluto da operação.
- **Geolocalização do navegador** — exige HTTPS (atendido em `test.cezar.dev`) e permissão; não é
  integração de backend.

## Segurança e privacidade

- Ameaças e controles:
  - injeção em query externa → `UriComponentsBuilder` com encoding; nunca concatenação;
  - vazamento de detalhes → `ErrorResponse` fixo; `reason`/corpo da Open-Meteo e mensagens Java nunca
    retornados; logs sem corpo externo;
  - abuso de tamanho → `name` ≤ 100 code points; coordenadas numéricas em faixa;
  - SSRF → base URLs só de `application.properties`; o usuário não controla host nem caminho;
  - XSS → interpolação Angular (escapa por padrão); nenhum `innerHTML`.
- Localização (BR-008, BR-009, RNF-003): só após clique; arredondada para 2 casas antes de sair do
  navegador; não armazenada em signal além da operação, nem em `localStorage`; o backend não registra
  `latitude`/`longitude` nem a URI de forecast em log. Risco residual: logs de acesso do Traefik/infra
  podem registrar a query string (fora do escopo do código; ver Riscos).
- Sem segredos (Open-Meteo sem chave); URLs `test.cezar.dev` são públicas — nenhum dado real de cliente.
- RBAC/tenancy/auditoria: não aplicável.

## Observabilidade e operação

- Logs (SLF4J, já presente no Boot): nos clients, `WARN` em cada falha traduzida, com API
  (`geocoding`/`forecast`), categoria (`status=<n>` | `timeout` | `io` | `invalid-body`) e duração;
  **sem** query string, coordenadas, corpo de resposta ou stack trace completo. Nome da cidade não é
  registrado (minimização).
- Métricas/traces/alertas: não há Actuator nem dependência de observabilidade (proibido adicionar); fora
  de escopo.
- Health check: `GET /healthcheck` inalterado (não consulta a Open-Meteo, para não acoplar a saúde da app
  à do provedor).
- Runbook (dev): falhas 502/504 recorrentes → `docker compose logs backend` e procurar os `WARN` dos
  clients; conferir conectividade de saída do container até `*.open-meteo.com`.

## Estratégia de testes

Os IDs `SCN-*`/`TST-*` são atribuídos no passo 03; abaixo, o que cada camada precisa provar.

- Unitários (backend, JUnit 5):
  - `services/WeatherServiceTest`: tabela WMO completa + `null`/desconhecidos; tabela de direção com
    todos os limites listados; validação de coordenadas (ausente, vazio, `abc`, `NaN`, `Infinity`,
    -90.0001, 90, -180, 180.0001); mapeamento completo; `current` ausente e `time` ausente → 502;
    campos ausentes → `null`; `isDay` 1/0/2/ausente.
  - `services/CityServiceTest` (clients mockados com Mockito): `null`, `""`, `"  "`, `" a "`,
    **`"\u00A0a\u00A0"` → inválido** e `"\uFEFFab\u2007"` → `"ab"` válido (casos de paridade), 2 e 100
    code points válidos, 101 inválido, nome com emoji contado por code point; `results` ausente/vazio →
    404; só `AIRP`/`ISL` → 404; mistura PPL/AIRP mantém só PPL* e a ordem; `feature_code` ausente
    mantido; 12 itens (corta em 10); item sem `name` → 502; mapeamento `admin1/admin2`.
  - `clients/GeocodingApiClientTest` e `clients/ForecastApiClientTest` com
    `MockRestServiceServer.bindTo(RestClient.builder())`: URL e query exatas (inclusive "São José"
    codificado uma vez — conferir o valor esperado pelo matcher `queryParam`, que compara a forma
    codificada); JSON com campos desconhecidos aceito; `{"generationtime_ms":0.1}` → `results` nulo;
    `withServerError()` → `UpstreamException`; `withStatus(BAD_REQUEST)` com `{"error":true,"reason":…}`
    → `UpstreamException`; corpo inválido (`"not json"`) → `UpstreamException`; `SocketTimeoutException`
    → `UpstreamTimeoutException`; `ConnectException` → `UpstreamException`.
- Unitários (frontend, Vitest):
  - `city-name.spec.ts`: **mesma matriz** de normalização/limites do `CityServiceTest`, inclusive
    `"\u00A0a\u00A0"` → inválido (paridade; contar code points com `[...s].length`, nunca `.length`).
  - `weather-messages.spec.ts`: `toFailure` para cada `code`/`status`/`TimeoutError`/geo `code` 1–3.
  - `weather-api.spec.ts`: `provideHttpClient()` + `provideHttpClientTesting()`; URL `api/cities` /
    `api/weather` (relativas, sem `/`), verbo GET, `params`; `httpMock.verify()` no `afterEach`.
  - `browser-location.spec.ts`: `navigator.geolocation` falso (stub) — sucesso, erro com `code`,
    callback tardio após unsubscribe ignorado; `isAvailable()` `false` com `PLATFORM_ID` `'server'`.
  - Componentes de apresentação: renderização por `setInput`, `output` emitido, textos PT-BR,
    `role="alert"`, `[ DIA ]`/`[ NOITE ]`, `—` em nulos, desambiguação por `subregion`, `16:00` a partir
    da string.
  - `weather-panel.spec.ts`: `WeatherApi` e `BrowserLocation` mockados (`vi.fn()` com `of`/`throwError`/
    `Subject`); cada transição da tabela; 1 cidade encadeia clima; retry repete a operação (busca,
    escolha e localização); resposta antiga ignorada (dois `Subject`s, emitir o primeiro depois);
    submit inválido durante `loading` ignora resposta tardia; teto de 15 s com `vi.useFakeTimers()`
    (`vi.advanceTimersByTime(15_000)` → `timeout`); `locate` com posição pendente aos 15 s →
    `geo-unavailable` com retry, e com posição obtida e clima pendente aos 15 s → `timeout`;
    `INVALID_COORDINATES` → `unavailable` sem botão de retry; `data-theme` `day`/`night` e permanência após erro;
    `aria-busy`; coordenadas arredondadas passadas a `getWeather`; geolocalização não chamada sem clique.
  - `app.spec.ts`: renderiza `app-weather-panel` (com `provideHttpClient()` + `provideHttpClientTesting()`).
- Integração (backend): `controllers/CityControllerTest`, `controllers/WeatherControllerTest` com
  `@WebMvcTest(XController.class)` + `@Import(ApiExceptionHandler.class)` + `@MockitoBean` do **service**
  — cada status (200/400/404/502/504) e JSON exato do `ErrorResponse` (`$.code`, `$.message`, e ausência
  de outros campos); JSON de sucesso com campos `null` presentes. `controllers/HealthCheckControllerTest`
  (`@WebMvcTest`) → `200 "OK"`. `AirqualityApplicationTests.contextLoads` continua passando (valida a
  fiação dos dois beans `RestClient` e das properties, sem chamada de rede).
  **Não** usar `@SpringBootTest` com override `@Primary` de `RestClient`: com injeção por `@Qualifier`,
  o bean primário não substitui os qualificados e o teste iria à Open-Meteo real.
- Contrato: os testes de controller fixam nomes/tipos/nulabilidade do JSON; os tipos TS de
  `weather-api.ts` espelham as tabelas desta seção e os fixtures do `weather-api.spec.ts` reutilizam os
  exemplos JSON acima.
- BDD/E2E: sem framework E2E no projeto (proibido adicionar); os cenários `.feature` do passo 03 são
  provados pelos testes acima e pelo QA manual.
- Performance/segurança: teto de 15 s (fake timers); varredura `validar-entrega` (sem `http(s)://` em
  `src/app`, sem `/api/` absoluto, sem CORS, sem cor hardcoded, sem `box-shadow`); teste de que corpo de
  erro não contém `reason` da Open-Meteo.
- Verificação pós-deploy (QA, ambiente de dev via `compose.yaml`, PRM-008):
  `https://test.cezar.dev/<VERSION>/api/healthcheck` → `OK`; "Pindamonhangaba" → sucesso direto
  (resultado único na sondagem de 2026-10-20; reconfirmar no QA); "Recife" → lista (4 "Recife", sem
  aeroporto/ilha); "São José" → lista com desambiguação por `subregion`; "xyzqwkk" → não encontrada; "a" → inválido sem requisição na aba Rede; localização
  concedida/negada; temas dia/noite (cidade onde é noite no momento do QA); 320 px sem rolagem
  horizontal; teclado e leitor de tela; `prefers-reduced-motion`.

## Sequenciamento

1. **Backend — fundação de erros**: `ErrorResponse`, exceções, `ApiExceptionHandler`, teste do
   `HealthCheckController`.
2. **Backend — clients**: properties, `OpenMeteoConfig`, DTOs externos, `GeocodingApiClient`,
   `ForecastApiClient` + testes `MockRestServiceServer`.
3. **Backend — `/cities`**: `CityResponse`, `CityService`, `CityController` + testes.
4. **Backend — `/weather`**: `WeatherResponse`, `WeatherService` (traduções), `WeatherController` +
   testes. (3 e 4 são independentes entre si após 2.)
5. **Frontend — base**: `index.html`, `styles.css` (tokens), `provideHttpClient(withFetch())`.
6. **Frontend — dados**: `weather-api.ts`, `browser-location.ts`, `city-name.ts`,
   `weather-messages.ts` + testes (dependem só do contrato desta TechSpec; paralelizáveis com 1–4).
7. **Frontend — apresentação**: `CitySearchForm`, `CityOptions`, `WeatherCard`, `WeatherSkeleton`,
   `StatusMessage` + testes.
8. **Frontend — orquestrador**: `WeatherPanel`, `app.ts`/`app.html`/`app.spec.ts` + testes.
9. **Validação**: skill `validar-entrega`, subida via `compose.yaml` e QA (passo 08).

## Riscos e mitigação

- **Risco:** Open-Meteo lenta/fora do ar deixa o painel travado. **Mitigação:** timeouts 2 s/4 s por
  chamada, teto de 15 s por operação no frontend, estados `timeout`/`unavailable` com retry.
  **Sinal de rollback:** teste de fake timers falhando ou QA vendo "carregando" > 15 s.
- **Risco:** linhas idênticas na lista de homônimas ("São José — Santa Catarina, Brasil" ×2).
  **Mitigação:** `subregion` (`admin2`) no contrato e sufixo só quando há colisão (PRM-020).
  **Sinal de rollback:** dev rejeitar PRM-020 → remover o campo antes do passo 06 (contrato ainda não
  publicado).
- **Risco:** a Geocoding casa por prefixo e devolve homônimas para quase qualquer nome real (sondagem:
  Recife 7, Curitiba 9, São Paulo 10), então "1 → clima direto" (BR-002) raramente acontece e o usuário
  quase sempre vê a lista. **Mitigação:** filtro `feature_code` PPL* (remove aeroportos/ilhas); BR-002
  mantido como está; auto-seleção (PRM-021) rejeitada pelo dev em 2026-10-20 (PRM-022). **Sinal de rollback:** QA ou
  dev considerar a lista obrigatória um atrito inaceitável → adotar PRM-021 antes do passo 06.
- **Risco:** divergência de validação cliente × servidor (NBSP, emoji). **Mitigação:** mesmo conjunto
  explícito de espaços (`White_Space` + U+FEFF) e contagem por code points nos dois lados, com a mesma
  matriz de testes. **Sinal:** 400 `INVALID_CITY_NAME` vindo do
  backend para entrada aceita pelo cliente (estado `invalid` após `loading`).
- **Risco:** teste de backend acessar a rede real. **Mitigação:** controllers com `@MockitoBean` do
  service; clients só com `MockRestServiceServer` sem `requestFactory(...)` depois do `bindTo`; sem
  `@SpringBootTest` que faça requisição. **Sinal:** teste lento/instável sem rede.
- **Risco:** horário exibido no fuso do navegador. **Mitigação:** exibir pela string, proibido
  `new Date(observedAt)`; teste com `observedAt` fixo. **Sinal:** QA vê hora diferente da hora local da
  cidade.
- **Risco:** acesso a `navigator`/HTTP no prerender quebrando o build SSR. **Mitigação:**
  `BrowserLocation.isAvailable()` com `isPlatformBrowser`, chamadas só em handlers; `npm run build` no
  gate. **Sinal:** build falha ou erro de hidratação no console.
- **Risco:** coordenadas em logs de acesso da infraestrutura (query string). **Mitigação:**
  arredondamento para 2 casas; nada logado pelo backend; infraestrutura fora de escopo. **Sinal:** dev
  exigir zero coordenada em log → reabrir ADR-001 (alternativa POST).
- **Risco:** Jackson 3 falhar com campo desconhecido/nulo em primitivo. **Mitigação:**
  `@JsonIgnoreProperties(ignoreUnknown = true)` e wrappers nos records externos; teste com campo extra.
- **Risco:** perguntas abertas do PRD respondidas de forma diferente (ver Premissas). **Mitigação:**
  impacto mapeado abaixo; decisões isoladas em componentes removíveis.

## Premissas e perguntas abertas herdadas

Defaults confirmados pelo dev em 2026-10-20 (PRM-022; PRD, perguntas 1–4). O mapa abaixo fica como
referência de impacto para um incremento futuro que os altere:

- **PRM-016 / pergunta 1 (localização sem busca reversa → "Sua localização")**: se o dev retirar a
  geolocalização, saem `browser-location.ts`, a operação `locate`, os estados `geo-denied`/
  `geo-unavailable`, o botão "Usar minha localização" e os testes correspondentes; `GET /weather` por
  coordenadas continua necessário (escolha na lista).
- **PRM-015 / pergunta 2 (só por clique)**: disparo automático exigiria chamar `locate` em
  `afterNextRender` e muda BR-008 e os cenários de QA.
- **PRM-017 / pergunta 3 (15 s)**: muda a constante `timeout(15_000)` e o orçamento de
  `geolocation timeout`/`read-timeout` da seção Integrações.
- **PRM-012 e PRM-014 / pergunta 4 (campos e lista)**: mudam `WeatherResponse`, `ForecastApiClient`
  (`current=`), `WeatherCard`; escolher "primeira cidade" removeria `CityOptions` e o estado `choosing`.

Nova premissa desta TechSpec:

- **PRM-020**: o contrato `CityResponse` inclui `subregion` (Open-Meteo `admin2`, nullable), exibido na
  lista de homônimas apenas quando duas opções têm o mesmo nome, região e país — vai além da redação de
  RF-004 ("nome, região/estado e país") e foi **aceita pelo dev em 2026-10-20** (PRM-022). Motivo: a sondagem de
  2026-10-20 mostrou duas "São José — Santa Catarina, Brasil" indistinguíveis sem `admin2`.
- **PRM-021** (proposta **rejeitada pelo dev em 2026-10-20**, PRM-022): auto-selecionar a cidade quando houver
  exatamente **uma** candidata cujo nome normalizado (sem acento, sem caixa) é igual ao buscado *e* com
  `feature_code` `PPLC`/`PPLA` (capital) — ex.: "Recife" iria direto a Recife/PE. Mudaria BR-002;
  fica fora deste incremento. Vale BR-002 literal + filtro PPL*.

## Alternativas descartadas

- Endpoint único `GET /weather?city=` com resposta polimórfica (clima ou lista) — ver ADR-001.
- `GET /weather?cityId=` usando `/v1/get?id=` da Geocoding — uma chamada externa extra por consulta e
  não serve à geolocalização (sem cidade).
- `POST /weather` com coordenadas no corpo (tirar coordenadas da query string) — foge do padrão REST de
  leitura e das skills; mitigação por arredondamento é suficiente para dev público sem dado real.
- Retry automático no backend ou no frontend — consumiria o orçamento de 15 s e mascararia falhas;
  substituído por retry manual (ADR-002).
- Handler genérico `@ExceptionHandler(Exception.class)` → 500 — capturaria 404/405 do framework.
- `@SpringBootTest` com `@Primary RestClient` para testes ponta a ponta — não substitui beans
  injetados por `@Qualifier`; risco de rede real.
- Direção do vento em 16 pontos — mais rótulos sem ganho para o usuário do painel; 8 pontos permitidos
  por BR-007.
- Formatar horário com `Date`/`DatePipe` — aplicaria o fuso do navegador.
- Bean Validation para parâmetros — proibido gerar o corpo de erro por ele (`backend.md`).

## ADRs

- `adrs/adr-001-recursos-cities-e-weather.md` — dois recursos (`/cities` e `/weather` por coordenadas)
  com orquestração no frontend.
- `adrs/adr-002-orcamento-de-tempo-e-retry.md` — timeouts por chamada, teto de 15 s por operação no
  frontend e retry manual por operação do usuário.

## Mapa das lacunas de `prompt_1.md`

| Lacuna de `prompt_1.md`                                         | Onde está o valor exato |
|-----------------------------------------------------------------|-------------------------|
| Endpoint(s): caminho, parâmetros, validação, campos/tipos/nulos | Backend → Endpoints, Validação; Contratos entre camadas |
| Tabela de erros (status, `code`, mensagem PT-BR)                | Backend → Erros |
| Chaves de `application.properties` e valores                    | Backend → Clients e configuração |
| Nomes de classes do backend                                     | Componentes e responsabilidades; Backend → DTOs do JSON externo |
| Traduções `weather_code` e graus → direção, com limites         | Backend → Tabelas de tradução |
| Cidades homônimas                                               | Fluxo de dados; Frontend → `CityOptions`; PRM-020 |
| Tema `day`/`night` e marcadores ASCII                           | Frontend → Estado do orquestrador (`theme`), `WeatherCard`, `styles.css` |
| Nomes de arquivos, classes e seletores do frontend              | Frontend → Arquivos; Componentes de apresentação |
| Estados, mensagens, retry e geolocalização                      | Frontend → Estado do orquestrador, Mensagens da UI, `BrowserLocation` |
