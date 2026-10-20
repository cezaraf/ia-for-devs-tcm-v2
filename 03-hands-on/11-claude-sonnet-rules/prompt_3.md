<persona>
Você é um Engenheiro de Software Full-Stack Sênior com especialidade em Java 25/Spring Boot 4 (RestClient, records, tratamento centralizado de erros), Angular 21 (componentes standalone, signals, SSR) e integração com APIs externas, construindo interfaces fiéis a um design system. Segue estritamente todos os requisitos definidos, as regras de `.claude/rules/` e as skills de `.claude/skills/`, e faz as entregas de acordo com o proposto.
</persona>

<task>
  Painel de clima atual por cidade (Open-Meteo)
</task>

<goal>
  O usuário digita o nome de uma cidade (ou usa a localização do navegador), escolhe entre as cidades homônimas quando houver mais de uma e vê o clima atual daquele local num painel terminal-like, com tema claro quando é dia e escuro quando é noite no local consultado.
</goal>

<requirements>
  Negócio:
  - Buscar cidades pelo nome: até 10 resultados, cada um com nome, estado/região e país, para desambiguar homônimos.
  - Se a busca retornar exatamente 1 cidade, carregar o clima dela direto; se retornar 2 ou mais, exibir a lista para o usuário escolher; se retornar 0, informar "cidade não encontrada".
  - Exibir o clima atual da cidade escolhida: temperatura (destaque), condição em PT-BR, sensação térmica, umidade relativa, vento (velocidade + direção cardeal), precipitação, indicação dia/noite e horário local da medição.
  - Geolocalização (opcional, só no navegador): ao abrir a página, tentar obter a posição automaticamente; também oferecer o botão "[usar minha localização]" para tentar de novo. Com a posição, buscar o clima por coordenadas e rotular o local como "Sua localização" (a Geocoding API da Open-Meteo não faz busca reversa; não exibir nome de cidade nesse caso).
  - Tema da página inteira segue o `isDay` do local consultado: dia → `data-theme="day"`, noite → `data-theme="night"`. Antes da primeira consulta, durante carregamento sem resultado anterior e em erro, usar `day`.
  - Unidades fixas: °C, km/h, mm, %; idioma PT-BR (nomes de cidades com `language=pt`).

  Arquitetura (o que é específico desta funcionalidade; camadas e convenções estão nas regras):
  - Backend:
    - `controllers/CityController` → `GET /cities`; `controllers/WeatherController` → `GET /weather`; `controllers/ApiExceptionHandler`.
    - `services/CityService` e `services/WeatherService`: validação, mapeamento para DTOs, tradução de `weather_code` (WMO) para PT-BR e de graus para ponto cardeal.
    - `clients/OpenMeteoClient`: única classe que chama a Open-Meteo (Geocoding e Forecast).
    - `application.properties`: `open-meteo.geocoding-url`, `open-meteo.forecast-url`, `open-meteo.connect-timeout=3s`, `open-meteo.read-timeout=5s`.
    - `dtos/`: `CityResponse`, `WeatherResponse`, `ErrorResponse` e records internos para o JSON da Open-Meteo.
    - `exceptions/`: `InvalidRequestException`, `CityNotFoundException`, `UpstreamException`, `UpstreamTimeoutException`.
  - Frontend, em `src/app/weather/`:
    - `weather-api.ts` (`WeatherApi`): chama `api/cities` e `api/weather`.
    - Componentes listados abaixo; `WeatherPanel` é o orquestrador, os demais são de apresentação.
    - `app.html`: substituir todo o placeholder do Angular por `<app-weather-panel />`, mantendo `<router-outlet />` (e ajustar `app.spec.ts`).
    - `src/index.html`: `<title>Clima</title>`.

  UI/UX — Componentes (arquivos `src/app/weather/<nome>.ts|.html|.css`):
  - `WeatherPanel` (`weather-panel.ts`, `app-weather-panel`): página inteira; título `[clima]`; orquestra o estado (`idle | searching | choosing | loading | success | error`); aplica `data-theme` no seu elemento raiz; decide qual filho exibir abaixo do formulário.
  - `CitySearchForm` (`city-search-form.ts`, `app-city-search-form`): label visível "cidade", input de texto, botão primário "buscar" e botão secundário "[usar minha localização]" (oculto se `navigator.geolocation` não existir). Envia com Enter ou clique; botões desabilitados enquanto `searching`/`loading`.
  - `CityList` (`city-list.ts`, `app-city-list`): até 10 resultados; cada linha é um `<button>` com `[+] <nome> — <estado>, <país>` (omitir partes nulas). Clique ou Enter seleciona.
  - `WeatherCard` (`weather-card.ts`, `app-weather-card`): rótulo do local (`<nome>, <estado>, <país>` ou "Sua localização"), badge `[ DIA ]`/`[ NOITE ]`, temperatura arredondada para inteiro + "°C", condição, grade de detalhes como linhas ASCII (`[+] sensação  24°C`, `[+] umidade  78%`, `[+] vento  12 km/h NE`, `[+] precipitação  0.0 mm`) e rodapé `medido às HH:mm (hora local)`.
  - `ErrorMessage` (`error-message.ts`, `app-error-message`): `[x]` + mensagem; botão secundário "tentar novamente" quando a ação for repetível.
  - `WeatherCardSkeleton` (`weather-card-skeleton.ts`, `app-weather-card-skeleton`): mesmas dimensões, padding e grade do `WeatherCard`, com blocos `--color-surface-card`.

  UI/UX — Especificidades visuais destes componentes (tokens gerais em `design-system.md`):
  - Peso 700: título `[clima]`, temperatura, rótulo do local. Peso 500: botões, condição, badge, item selecionado. Peso 400: demais textos.
  - Temperatura: `font-size: clamp(28px, 6vw, 38px)`, peso 700, `line-height: 1.5`, `letter-spacing: 0`.
  - Título `[clima]`, rótulo do local, condição e detalhes: 16px. Badge, label "cidade" e rodapé do card: 14px com `line-height: 2`.
  - `CityList`: `max-height: 200px` (5 linhas visíveis de 40px), `overflow-y: auto`, borda 1px `--color-hairline`, `--radius-container`.
  - `WeatherCard` e `WeatherCardSkeleton`: `min-height: 320px`; padding 48px 32px (24px 16px abaixo de 640px); tema `day`: fundo `--color-bg` com borda 1px `--color-hairline`; tema `night`: fundo `--color-surface` sem borda. Grade de detalhes em 2 colunas com gap 16px (1 coluna abaixo de 640px); linha de detalhe com padding 8px 0.
  - Badge: padding 4px 8px, fundo `--color-text`, texto `--color-bg`, `--radius-control`.
  - Distância entre formulário e área de resultado/erro: 32px.
  - Texto do estado `idle` em `--color-text-mute`.

  UI/UX — Comportamento:
  - Conteúdo por estado: `idle` (só formulário + "digite uma cidade para ver o clima atual"), `searching` (skeleton), `choosing` (`CityList`), `loading` (skeleton), `success` (`WeatherCard`), `error` (`ErrorMessage`).
  - A área de resultado tem `aria-busy="true"` durante `searching`/`loading`.
  - Ao entrar em `choosing`, o foco vai para o primeiro item da lista.
  - Geolocalização automática ao abrir (só no navegador): `getCurrentPosition` com `timeout: 10000` e `maximumAge: 600000`. Se negada, indisponível ou expirada, ficar em `idle` sem mostrar erro.
  - Erros exibidos ao usuário:
    - Campo vazio ou com menos de 2 caracteres (validação no frontend, sem chamar o backend): "Digite ao menos 2 caracteres." — sem retry; foco volta ao input.
    - `404 CITY_NOT_FOUND`: "Cidade não encontrada. Verifique o nome e tente novamente." — sem retry; foco volta ao input.
    - `400` do backend: "Requisição inválida." — sem retry.
    - `502 UPSTREAM_ERROR`: "O serviço de clima está indisponível no momento." — "tentar novamente" repete a última ação (busca de cidades ou de clima).
    - `504 UPSTREAM_TIMEOUT`: "O serviço de clima demorou para responder." — "tentar novamente".
    - Falha de rede/status 0 ou qualquer outro status: "Não foi possível conectar ao servidor." — "tentar novamente".
    - Geolocalização pelo botão negada: "Permissão de localização negada. Digite uma cidade." — sem retry; foco vai para o input.
    - Geolocalização pelo botão indisponível/expirada: "Não foi possível obter sua localização." — "tentar novamente".
</requirements>

<api_contracts>
  APIs externas (consumidas somente por `clients/OpenMeteoClient`):
  - Open-Meteo Geocoding: `GET https://geocoding-api.open-meteo.com/v1/search?name={nome}&count=10&language=pt&format=json` — converte nome em lista de cidades com coordenadas. Sem resultados, a resposta não traz a chave `results`.
  - Open-Meteo Forecast: `GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto` — clima atual nas coordenadas.

  APIs de backend (no frontend: `api/cities` e `api/weather`; no Spring: `/cities` e `/weather`):
  - `GET /cities?name={nome}`: `name` obrigatório; aplicar `trim`; 2 a 100 caracteres.
  - Resposta 200: array JSON (1 a 10 itens) de objetos:
    - `id`: number (inteiro, id da Open-Meteo)
    - `name`: string
    - `admin1`: string | null (estado/região)
    - `country`: string | null
    - `countryCode`: string | null (ISO-3166 alpha-2)
    - `latitude`: number
    - `longitude`: number
  - `GET /weather?latitude={lat}&longitude={lon}`: ambos obrigatórios e numéricos; `latitude` de -90 a 90, `longitude` de -180 a 180.
  - Resposta 200: objeto JSON:
    - `latitude`: number
    - `longitude`: number
    - `timezone`: string (ex. `"America/Sao_Paulo"`)
    - `observedAt`: string (data-hora local ISO-8601 sem offset, ex. `"2026-10-13T14:15"`)
    - `temperature`: number (°C)
    - `apparentTemperature`: number (°C)
    - `relativeHumidity`: number (inteiro, %)
    - `precipitation`: number (mm)
    - `windSpeed`: number (km/h)
    - `windDirection`: number (inteiro, graus 0–360)
    - `windDirectionCardinal`: string (`"N"`, `"NE"`, `"E"`, `"SE"`, `"S"`, `"SO"`, `"O"`, `"NO"` — setores de 45° centrados no ponto)
    - `weatherCode`: number (inteiro, código WMO)
    - `condition`: string (PT-BR, tabela abaixo)
    - `isDay`: boolean
  - Tabela `weatherCode` → `condition`: 0 "Céu limpo"; 1 "Predominantemente limpo"; 2 "Parcialmente nublado"; 3 "Nublado"; 45 "Nevoeiro"; 48 "Nevoeiro com geada"; 51 "Garoa fraca"; 53 "Garoa moderada"; 55 "Garoa intensa"; 56 "Garoa congelante fraca"; 57 "Garoa congelante intensa"; 61 "Chuva fraca"; 63 "Chuva moderada"; 65 "Chuva forte"; 66 "Chuva congelante fraca"; 67 "Chuva congelante forte"; 71 "Neve fraca"; 73 "Neve moderada"; 75 "Neve forte"; 77 "Grãos de neve"; 80 "Pancadas de chuva fracas"; 81 "Pancadas de chuva moderadas"; 82 "Pancadas de chuva violentas"; 85 "Pancadas de neve fracas"; 86 "Pancadas de neve fortes"; 95 "Trovoada"; 96 "Trovoada com granizo fraco"; 99 "Trovoada com granizo forte"; qualquer outro "Condição desconhecida".
  - Códigos de erro (corpo `ErrorResponse` padrão):
    - 400 `INVALID_CITY_NAME`: `name` ausente, vazio após `trim`, com menos de 2 ou mais de 100 caracteres.
    - 400 `INVALID_COORDINATES`: `latitude`/`longitude` ausente, não numérica ou fora do intervalo.
    - 404 `CITY_NOT_FOUND`: Geocoding respondeu 200 sem `results` ou com lista vazia.
    - 502 `UPSTREAM_ERROR` e 504 `UPSTREAM_TIMEOUT`: conforme o mapeamento padrão de falhas da Open-Meteo (conexão > 3s ou leitura > 5s = timeout).
</api_contracts>

<acceptance_criteria>
  - Dado o painel em `idle`, quando o usuário busca uma cidade e `GET /cities` retorna exatamente 1 item (ex. `Curitiba`, `Paraná`, `Brasil`), então o clima é carregado direto, sem exibir lista, e o `WeatherCard` mostra "Curitiba, Paraná, Brasil", temperatura inteira em °C, condição em PT-BR, sensação, umidade, vento com ponto cardeal, precipitação e "medido às HH:mm".
  - Dado que a busca "Santa Cruz" retorna vários resultados, quando o backend responde, então a `CityList` mostra até 10 itens (5 visíveis, com scroll) com nome, estado e país, o foco vai para o primeiro item, e ao escolher um deles o clima daquela cidade é exibido.
  - Dado um local onde é noite (`isDay=false`), quando o clima é exibido, então a página inteira usa o tema `night` e o badge mostra `[ NOITE ]`; numa nova consulta com `isDay=true` volta ao tema `day` com `[ DIA ]`.
  - Dado o campo vazio ou com 1 caractere, quando o usuário clica em "buscar", então nenhuma requisição é feita e aparece "Digite ao menos 2 caracteres.".
  - Dado `GET /cities?name=a` ou `GET /weather?latitude=200&longitude=0`, quando chamados diretamente, então o backend responde 400 com `INVALID_CITY_NAME` ou `INVALID_COORDINATES`.
  - Dado um nome inexistente ("Xyzabcqwe"), quando o usuário busca, então o backend responde 404 `CITY_NOT_FOUND` e o painel mostra "Cidade não encontrada. Verifique o nome e tente novamente." sem botão de retry.
  - Dado que a Open-Meteo está fora do ar ou retorna 5xx, quando o usuário busca, então o backend responde 502 `UPSTREAM_ERROR`, o painel mostra a mensagem correspondente e "tentar novamente" repete a mesma ação.
  - Dado que a Open-Meteo não responde em 5s, quando o usuário busca, então o backend responde 504 `UPSTREAM_TIMEOUT` e o painel mostra "O serviço de clima demorou para responder." com "tentar novamente".
  - Dado que o backend está inacessível, quando o usuário busca, então o painel mostra "Não foi possível conectar ao servidor." com "tentar novamente".
  - Dado que o navegador concede a localização ao abrir a página, quando a página carrega, então o clima é exibido com o rótulo "Sua localização" sem nenhuma ação do usuário; se a permissão for negada, o painel fica em `idle` sem erro.
  - Dado que o usuário clicou em "[usar minha localização]" e negou a permissão, então aparece "Permissão de localização negada. Digite uma cidade." e o foco vai para o input.
  - Dado que o usuário faz duas buscas seguidas rapidamente, quando a resposta da primeira chega depois da segunda, então o painel mostra apenas o resultado da segunda.
  - Dado o carregamento, quando o skeleton é exibido, então ele ocupa as mesmas dimensões do `WeatherCard` (sem salto de layout) e o resultado tem `aria-busy="true"`.
  - Dado uma tela com menos de 640px, quando o painel é exibido, então o formulário empilha, o card usa padding 24px 16px e a grade de detalhes fica em 1 coluna, sem scroll horizontal.
  - Dado a renderização SSR, quando a página é gerada no servidor, então nenhuma chamada ao backend nem acesso a `navigator` acontece no servidor.
  - Testes: `./mvnw test` passa com testes de `CityService`, `WeatherService` (incluindo tabela WMO e ponto cardeal) e dos controllers cobrindo 200/400/404/502/504; `npm test` passa com testes do `WeatherApi` e do `WeatherPanel` cobrindo os fluxos 1 resultado, vários resultados, erro com retry e tema dia/noite.
</acceptance_criteria>

<constraints>
  - NÃO FAÇA (escopo desta funcionalidade): histórico de buscas, favoritos, previsão de dias futuros; o tema depende só de `isDay`.
  - Ao terminar, execute a skill `validar-entrega`.
</constraints>
