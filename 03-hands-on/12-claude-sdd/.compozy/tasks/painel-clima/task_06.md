---
status: completed
title: "Task 6.0: Frontend — camada de dados (WeatherApi, BrowserLocation, validação e mensagens)"
type: frontend
complexity: medium
dependencies: []
---

# Task 6.0: Frontend — camada de dados (WeatherApi, BrowserLocation, validação e mensagens)

## Rastreabilidade

- FEAT-003: Painel de busca e exibição do clima; FEAT-004: Clima pela localização do navegador
- RF/RNF: RF-001, RF-002, RF-005, RF-006, RF-008, RF-009, RNF-001, RNF-003, RNF-006, RNF-009
- BR: BR-001, BR-003, BR-004, BR-006, BR-008, BR-009
- Comportamento: "Busca por nome no painel"; "Carregamento, falhas e Tentar novamente"; "Clima pela localização do
  navegador"; "Renderização no servidor sem efeitos do navegador"
- SCN: SCN-021, SCN-022, SCN-023, SCN-024
- TST: TST-011, TST-012, TST-013, TST-014
- Impacto contratual: `impacto-contratual/clima/contrato.md` → comportamentos acima (lado do navegador)

## Limites

- Dentro do escopo (pasta `frontend/src/app/weather/`):
  - `weather-api.ts` → `WeatherApi` (`providedIn: 'root'`, único ponto de HTTP): `searchCities(name)` →
    `GET api/cities` com `params: { name }`; `getWeather(latitude, longitude)` → `GET api/weather`; interfaces
    `City`, `Weather`, `ApiError` idênticas às tabelas de "Contratos entre camadas";
  - `browser-location.ts` → `BrowserLocation`: `isPlatformBrowser` no inicializador de campo, `isAvailable()`,
    `getPosition()` como `Observable` com `{ enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 }`,
    erro `{ code }` e descarte de callback tardio após unsubscribe;
  - `city-name.ts`: `normalizeCityName`, `isValidCityName`, `CITY_NAME_MIN = 2`, `CITY_NAME_MAX = 100`
    (regex `White_Space` + U+FEFF, contagem `[...s].length`);
  - `weather-messages.ts`: `MESSAGES` com os textos exatos da TechSpec e `toFailure(error)`.
- Fora do escopo: componentes e orquestrador (tasks 07 e 08); arredondamento da coordenada (task 08).
- Arquivos esperados:
  - `frontend/src/app/weather/weather-api.ts` e `weather-api.spec.ts` (novos)
  - `frontend/src/app/weather/browser-location.ts` e `browser-location.spec.ts` (novos)
  - `frontend/src/app/weather/city-name.ts` e `city-name.spec.ts` (novos)
  - `frontend/src/app/weather/weather-messages.ts` e `weather-messages.spec.ts` (novos)
- Contratos afetados: consumo de `GET /cities` e `GET /weather` (JSON de sucesso e `ErrorResponse`).

## Dependências

- Nenhuma no grafo de tasks: implementa contra o contrato da TechSpec ("Contratos entre camadas"), não contra o
  código do backend. A compatibilidade real com as tasks 03 e 04 é provada no gate de validação e no QA.
- Os testes usam `provideHttpClient()` no próprio `TestBed`; não dependem do `app.config.ts` da task 05.

## Subtasks

- [x] Criar os quatro arquivos e suas specs.
- [x] TST-011 `weather-api.spec.ts`: `provideHttpClient()` + `provideHttpClientTesting()`; URL `api/cities` e
      `api/weather` relativas e sem barra inicial, verbo GET, `params` exatos; fixtures iguais aos exemplos JSON
      da TechSpec; `httpMock.verify()` no `afterEach`.
- [x] TST-012 `city-name.spec.ts`: mesma matriz do `CityServiceTest` (inclui `"\u00A0a\u00A0"` inválido,
      `"\uFEFFab\u2007"` → `"ab"`, 2/100/101 code points, emoji).
- [x] TST-013 `weather-messages.spec.ts`: `toFailure` para `CITY_NOT_FOUND`, `INVALID_CITY_NAME`,
      `INVALID_COORDINATES` (sem retry), `UPSTREAM_ERROR`, `UPSTREAM_TIMEOUT`, `status 0`, 5xx sem corpo,
      `TimeoutError`, geo `code` 1/2/3; nunca usa `error.message` do backend.
- [x] TST-014 `browser-location.spec.ts`: `navigator.geolocation` falso (stub) — sucesso, erro com `code`, callback
      tardio ignorado; `isAvailable()` `false` com `PLATFORM_ID` `'server'`.
- [x] Nomear cada `it` com o ID do cenário (ex.: `it('SCN-021 …')`).

## Critérios de sucesso

- Só `weather-api.ts` injeta `HttpClient`; nenhuma URL absoluta, `/api/` com barra ou host externo em `src/app`.
- Validação do nome no navegador aceita e rejeita exatamente os mesmos casos que o backend.
- `toFailure` cobre cada linha da tabela de transição de erro da TechSpec.
- Nenhum acesso a `navigator` no servidor.
- Nomes dos testes citam SCN-021 a SCN-024
- `cd frontend && npm test` verde, sem rede nem geolocalização real.
- Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd frontend && npm test` com as quatro specs verdes.
- Varreduras da skill `validar-entrega` para `https?://`, `/api/` e `inject(HttpClient)` sem ocorrência indevida.

## Evidências produzidas

- `cd frontend && npm test`: 5 arquivos, 44 testes verdes (4 specs novas + app.spec). Varreduras: `inject(HttpClient)` só em `weather-api.ts`; `https?://` e `/api/` só no `app.html` legado do scaffold (substituído na task 08).


