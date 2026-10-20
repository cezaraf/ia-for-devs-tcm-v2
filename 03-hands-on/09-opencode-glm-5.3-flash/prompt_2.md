<persona>
Você é um Engenheiro de Software Full-Stack Sênior com especialidade em Java 25/Spring Boot 4 (RestClient, records, tratamento centralizado de erros), Angular 21 (componentes standalone, signals, SSR) e integração com APIs externas, construindo interfaces fiéis a um design system. Segue estritamente todos os requisitos definidos e faz as entregas de acordo com o proposto.
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
  - Tema da página inteira segue o `isDay` do local consultado: dia → tema claro (`day`), noite → tema escuro (`night`). Antes da primeira consulta, durante carregamento sem resultado anterior e em erro, usar o tema `day`.
  - Unidades fixas: °C, km/h, mm, %; idioma PT-BR (nomes de cidades com `language=pt`).

  Arquitetura:
  - Backend existente em `backend/` (Spring Boot 4.0.6, Java 25), pacote base `com.cezar.dev.airquality`:
    - `controllers/CityController` → `GET /cities`; `controllers/WeatherController` → `GET /weather`; `controllers/ApiExceptionHandler` (`@RestControllerAdvice`) converte exceções no corpo de erro padrão.
    - `services/CityService` e `services/WeatherService`: validação de regra, mapeamento para DTOs, tradução de `weather_code` (WMO) para PT-BR e de graus para ponto cardeal.
    - `clients/OpenMeteoClient`: única classe que chama a Open-Meteo, usando `RestClient` (dependência `spring-boot-starter-restclient` já presente). URLs base e timeouts em `application.properties` (`open-meteo.geocoding-url`, `open-meteo.forecast-url`, `open-meteo.connect-timeout=3s`, `open-meteo.read-timeout=5s`).
    - `dtos/`: records `CityResponse`, `WeatherResponse`, `ErrorResponse` (e records internos para o JSON da Open-Meteo).
    - `exceptions/`: `InvalidRequestException`, `CityNotFoundException`, `UpstreamException`, `UpstreamTimeoutException`.
  - Frontend existente em `frontend/` (Angular 21, SSR habilitado), dentro de `src/app/weather/`:
    - `weather-api.ts` (`WeatherApi`, `@Injectable({ providedIn: 'root' })`): único ponto que faz HTTP; chama somente o backend.
    - Componentes listados em "UI/UX — Componentes". `WeatherPanel` orquestra estado e chamadas; os demais só recebem `input()` e emitem `output()`.
    - `app.html`: substituir todo o placeholder do Angular por `<app-weather-panel />`, mantendo `<router-outlet />`.
    - `app.config.ts`: adicionar `provideHttpClient(withFetch())`.
  - Fluxo de dados: Navegador → Backend (`/cities`, `/weather`) → Open-Meteo. O frontend nunca chama a Open-Meteo.
  - URLs do backend no frontend são relativas e sem barra inicial (`api/cities`, `api/weather`), resolvidas contra o `baseHref`. O Traefik remove o prefixo `/<base>/api` antes de chegar ao Spring; por isso os endpoints Spring são `/cities` e `/weather` (sem `/api`). Nunca fixar `/template/` ou outro prefixo no código.
  - SSR: `navigator.geolocation` e qualquer chamada HTTP só rodam no navegador (`afterNextRender` ou `isPlatformBrowser`). Nenhuma requisição ao backend durante a renderização no servidor.

  UI/UX — Componentes:
  - `WeatherPanel` (`weather-panel.ts`, seletor `app-weather-panel`): página inteira; contém o título `[clima]`, orquestra o estado (`idle | searching | choosing | loading | success | error`), aplica `data-theme="day|night"` no seu elemento raiz e decide qual componente filho exibir abaixo do formulário.
  - `CitySearchForm` (`city-search-form.ts`, `app-city-search-form`): label visível "cidade", input de texto, botão primário "buscar" e botão secundário "[usar minha localização]" (oculto se `navigator.geolocation` não existir). Envia com Enter ou clique; botões desabilitados enquanto `searching`/`loading`.
  - `CityList` (`city-list.ts`, `app-city-list`): lista dos resultados (até 10) com 5 linhas visíveis e scroll vertical; cada linha é um `<button>` com `[+] <nome> — <estado>, <país>` (omitir partes nulas). Clique ou Enter seleciona.
  - `WeatherCard` (`weather-card.ts`, `app-weather-card`): rótulo do local (`<nome>, <estado>, <país>` ou "Sua localização"), badge `[ DIA ]`/`[ NOITE ]`, temperatura arredondada para inteiro + "°C", condição, grade de detalhes como linhas ASCII (`[+] sensação  24°C`, `[+] umidade  78%`, `[+] vento  12 km/h NE`, `[+] precipitação  0.0 mm`) e rodapé `medido às HH:mm (hora local)`.
  - `ErrorMessage` (`error-message.ts`, `app-error-message`): `[x]` + mensagem; botão secundário "tentar novamente" quando a ação for repetível (ver Comportamento).
  - `WeatherCardSkeleton` (`weather-card-skeleton.ts`, `app-weather-card-skeleton`): skeleton com exatamente as mesmas dimensões, padding e grade do `WeatherCard`, usando blocos `--color-surface-card`.
  - Use nos arquivos gerados os nomes exatos definidos aqui (padrão Angular 21 do projeto: arquivo `nome.ts`/`nome.html`/`nome.css`, classe sem sufixo `Component`, como `app.ts`/`App`).

  UI/UX — Paleta de cores (defina como CSS variables em `src/styles.css`, por `[data-theme]`):
  - `day` (claro): fundo `#fdfcfc`, superfície `#f8f7f7` (input, hover de linha), superfície-card `#f1eeee` (skeleton, botão desabilitado), texto `#201d1d`, texto-corpo `#424245`, texto-mute `#646262`, hairline `rgba(15,0,0,0.12)`, hairline-strong `#646262`, acento `#201d1d`, texto-sobre-acento `#fdfcfc`, danger `#ff3b30`.
  - `night` (escuro): fundo `#201d1d`, superfície `#302c2c` (card, input, hover de linha), superfície-card `#302c2c`, texto `#fdfcfc`, texto-corpo `#9a9898`, texto-mute `#9a9898`, hairline `rgba(253,252,252,0.12)`, hairline-strong `#9a9898`, acento `#fdfcfc`, texto-sobre-acento `#201d1d`, danger `#ff3b30`.
  - Nomes dos tokens: `--color-bg`, `--color-surface`, `--color-surface-card`, `--color-text`, `--color-text-body`, `--color-text-mute`, `--color-hairline`, `--color-hairline-strong`, `--color-accent`, `--color-on-accent`, `--color-danger`.
  - Acento (ink/cream) só no preenchimento do botão primário, no contorno de foco e no item da lista com foco/hover. Nunca usar o azul `#007aff` nem warning/success. `--color-danger` aparece somente no marcador `[x]`; o texto da mensagem de erro usa `--color-text`.

  UI/UX — Tipografia:
  - JetBrains Mono 700: título `[clima]`, temperatura, rótulo do local.
  - JetBrains Mono 500: botões, condição do clima, badge, item selecionado.
  - JetBrains Mono 400: corpo, input, linhas de detalhe, lista, mensagens, rodapé do card.
  - Stack: `'JetBrains Mono', 'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace` no token `--font-mono`. Carregar via `<link>` do Google Fonts no `src/index.html` (pesos 400/500/700, `display=swap`).
  - Temperatura (maior destaque): `font-size: clamp(28px, 6vw, 38px)`, peso 700, `line-height: 1.5`, `letter-spacing: 0`.
  - Sem itálico, sem fonte proporcional.

  UI/UX — Dimensões e layout (únicos valores permitidos — não inventar fora desta lista):
  - Container: `max-width: 640px`, centralizado, padding lateral 16px; padding superior 96px (48px abaixo de 640px); padding inferior 48px. Abaixo de 640px ocupa 100% da largura menos o gutter.
  - Escala de espaçamento: 4, 8, 12, 16, 24, 32, 48, 96 px.
  - Input: altura 40px, padding 8px 12px, borda 1px `--color-hairline`; em foco, fundo `--color-bg` e borda 1px `--color-accent`.
  - Botão primário e secundário: altura 40px, padding 4px 20px; secundário com borda 1px `--color-hairline-strong` e fundo transparente.
  - Formulário: input + botões numa linha com gap 8px; abaixo de 640px empilha (input 100%, botões 100% cada, gap 8px).
  - `CityList`: linha com altura 40px e padding 8px 12px; `max-height: 200px` (5 linhas), `overflow-y: auto`; borda 1px `--color-hairline`; separador 1px `--color-hairline` entre linhas.
  - `WeatherCard` e `WeatherCardSkeleton`: `min-height: 320px`; padding 48px 32px (24px 16px abaixo de 640px); fundo `--color-bg` com borda 1px `--color-hairline` no tema `day`; fundo `--color-surface` sem borda no tema `night`. Grade de detalhes: 2 colunas com gap 16px (1 coluna abaixo de 640px); linha de detalhe com padding 8px 0.
  - Badge: padding 4px 8px, fundo `--color-text`, texto `--color-bg`.
  - Distância entre formulário e área de resultado/erro: 32px.
  - Arredondamentos: `--radius-control: 4px` (input, botões, badge); `--radius-container: 0px` (card, skeleton, lista, mensagem de erro).
  - Sombra: nenhuma (nenhum `box-shadow`, gradiente ou blur).
  - Tamanhos de texto: título `[clima]` 16px; temperatura `clamp(28px, 6vw, 38px)`; rótulo do local 16px; condição 16px; detalhes 16px; input e botões 16px (`line-height: 2` nos botões); badge, label "cidade" e rodapé do card 14px (`line-height: 2`); demais `line-height: 1.5`.
  - Breakpoint: 640px (max-width 639px) — padding superior 96→48px, formulário empilha, padding do card 48/32→24/16px, grade de detalhes 2→1 coluna.

  UI/UX — Repetibilidade (geração determinística):
  - Use exatamente os valores definidos nesta spec; não inventar cor, tamanho, sombra ou fonte fora dos tokens.
  - Expor todos os tokens (cor, raio, fonte, espaçamento) como variáveis CSS em `src/styles.css` — nunca hardcoded dentro dos componentes; cada componente usa seu próprio `.css` com `var(--…)`.
  - Nenhuma biblioteca de UI, ícones ou fontes além de JetBrains Mono (Google Fonts). O Tailwind já instalado permanece apenas com o `@import 'tailwindcss'` existente; não usar classes utilitárias nem `@theme`.
  - Ícones são exclusivamente marcadores ASCII de texto: `[+]`, `[x]`, `[ DIA ]`, `[ NOITE ]`.

  UI/UX — Comportamento:
  - Estados obrigatórios do `WeatherPanel`: `idle` (só formulário + texto mute "digite uma cidade para ver o clima atual"), `searching` (skeleton), `choosing` (`CityList`), `loading` (skeleton), `success` (`WeatherCard`), `error` (`ErrorMessage`).
  - Nova busca cancela a anterior (`switchMap` ou equivalente); respostas atrasadas de buscas antigas nunca sobrescrevem o estado.
  - Geolocalização automática (ao abrir, só no navegador): `getCurrentPosition` com `timeout: 10000` e `maximumAge: 600000`. Se negada, indisponível ou expirada, ficar em `idle` sem mostrar erro.
  - Erros exibidos ao usuário:
    - Campo vazio ou com menos de 2 caracteres (validação no frontend, sem chamar o backend): "Digite ao menos 2 caracteres." — sem botão de retry; foco volta ao input.
    - `404 CITY_NOT_FOUND`: "Cidade não encontrada. Verifique o nome e tente novamente." — sem botão de retry; foco volta ao input.
    - `400` do backend: "Requisição inválida." — sem botão de retry.
    - `502 UPSTREAM_ERROR`: "O serviço de clima está indisponível no momento." — botão "tentar novamente" repete a última ação (busca de cidades ou de clima).
    - `504 UPSTREAM_TIMEOUT`: "O serviço de clima demorou para responder." — botão "tentar novamente".
    - Falha de rede/status 0 ou qualquer outro status: "Não foi possível conectar ao servidor." — botão "tentar novamente".
    - Geolocalização pelo botão negada: "Permissão de localização negada. Digite uma cidade." — sem retry; foco vai para o input.
    - Geolocalização pelo botão indisponível/expirada: "Não foi possível obter sua localização." — botão "tentar novamente".
  - Acessibilidade:
    - Foco visível em todo elemento interativo: `outline: 2px solid var(--color-accent)`, `outline-offset: 2px`; nunca `outline: none` sem substituto.
    - Contraste mínimo WCAG AA (4,5:1) para texto nos dois temas (os pares de cor acima já atendem).
    - Input com `<label for>` visível; área de resultado com `aria-live="polite"` e `aria-busy="true"` durante `searching`/`loading`; `ErrorMessage` com `role="alert"`.
    - `CityList` navegável por teclado (Tab + Enter); ao entrar em `choosing`, foco vai para o primeiro item.
    - Skeleton com animação de opacidade (1 → 0.5, 1.2s, infinita), desativada em `prefers-reduced-motion: reduce`. Troca de tema sem transição animada.
    - `<html lang="pt-BR">`.
</requirements>

<api_contracts>
  APIs externas (consumidas somente pelo backend, via `clients/OpenMeteoClient`):
  - Open-Meteo Geocoding: `GET https://geocoding-api.open-meteo.com/v1/search?name={nome}&count=10&language=pt&format=json` — converte nome em lista de cidades com coordenadas. Sem resultados, a resposta não traz a chave `results`.
  - Open-Meteo Forecast: `GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto` — clima atual nas coordenadas.

  APIs de backend (chamadas pelo frontend como `api/cities` e `api/weather`; no Spring, sem prefixo):
  - `GET /cities?name={nome}`: busca cidades. `name` obrigatório; aplicar `trim`; 2 a 100 caracteres.
  - Resposta de sucesso (200): array JSON (1 a 10 itens) de objetos:
    - `id`: number (inteiro, id da Open-Meteo)
    - `name`: string
    - `admin1`: string | null (estado/região)
    - `country`: string | null
    - `countryCode`: string | null (ISO-3166 alpha-2)
    - `latitude`: number
    - `longitude`: number
  - `GET /weather?latitude={lat}&longitude={lon}`: clima atual. Ambos obrigatórios e numéricos; `latitude` de -90 a 90, `longitude` de -180 a 180.
  - Resposta de sucesso (200): objeto JSON:
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
  - Respostas de erro (ambos os endpoints), corpo `ErrorResponse`: `{ "code": string, "message": string }` (mensagem em PT-BR, sem stack trace nem conteúdo bruto da Open-Meteo):
    - 400 `INVALID_CITY_NAME`: `name` ausente, vazio após `trim`, com menos de 2 ou mais de 100 caracteres.
    - 400 `INVALID_COORDINATES`: `latitude`/`longitude` ausente, não numérica ou fora do intervalo.
    - 404 `CITY_NOT_FOUND`: Geocoding respondeu 200 sem `results` ou com lista vazia.
    - 502 `UPSTREAM_ERROR`: Open-Meteo respondeu status diferente de 2xx, corpo inválido/incompleto ou a conexão falhou.
    - 504 `UPSTREAM_TIMEOUT`: conexão > 3s ou leitura > 5s.
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
  - Testes: `./mvnw test` passa com testes de `CityService`, `WeatherService` (incluindo tabela WMO e ponto cardeal) e dos controllers cobrindo 200/400/404/502/504, usando `MockRestServiceServer` (sem chamar a Open-Meteo real); `npm test` (vitest) passa com testes do `WeatherApi` (`HttpTestingController`) e do `WeatherPanel` cobrindo os fluxos 1 resultado, vários resultados, erro com retry e tema dia/noite.
</acceptance_criteria>

<constraints>
  - FAÇA: backend com `RestClient` (Spring Boot 4), DTOs como `record`, validação no service/controller com exceções próprias tratadas no `ApiExceptionHandler`; URLs e timeouts da Open-Meteo em `application.properties`.
  - FAÇA: siga os padrões existentes do projeto — pacote `com.cezar.dev.airquality` com o subpacote `controllers` já existente (como `HealthCheckController`); no Angular 21, componentes standalone com `signal`/`computed`, `input()`/`output()`, `inject()`, controle de fluxo `@if`/`@for`, arquivos `nome.ts`/`.html`/`.css` sem sufixo `.component`.
  - FAÇA: `provideHttpClient(withFetch())` em `app.config.ts`; chamadas e geolocalização só no navegador; URLs relativas `api/cities` e `api/weather`.
  - FAÇA: `<html lang="pt-BR">` e `<title>Clima</title>` no `src/index.html`.
  - NÃO FAÇA: alterar `compose.yaml`, adicionar `ports:`, Dockerfile, configuração de CORS (frontend e backend estão na mesma origem via Traefik), nem mudar `angular.json`/`baseHref`.
  - NÃO FAÇA: autenticação, cache, banco de dados, histórico de buscas, favoritos, previsão de dias futuros, alternância de unidades, internacionalização ou dark mode por `prefers-color-scheme` (o tema depende só de `isDay`).
  - NÃO FAÇA: adicionar dependências (Angular Material, bibliotecas de ícones, axios, Lombok, WireMock etc.) além do `<link>` da JetBrains Mono; não usar classes utilitárias do Tailwind.
  - NUNCA: o frontend chamar a Open-Meteo diretamente; fixar `/template/` ou outro prefixo de rota no código; expor stack trace ou resposta bruta da Open-Meteo ao cliente; quebrar `GET /healthcheck`; usar cor, tamanho, sombra ou fonte fora dos tokens desta spec.
</constraints>
