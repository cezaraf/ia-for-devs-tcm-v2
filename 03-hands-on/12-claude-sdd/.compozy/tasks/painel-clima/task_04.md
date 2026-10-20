---
status: completed
title: "Task 4.0: Backend — endpoint GET /weather e tabelas de tradução"
type: backend
complexity: medium
dependencies: [task_02]
---

# Task 4.0: Backend — endpoint GET /weather e tabelas de tradução

## Rastreabilidade

- FEAT-002: Condições atuais por coordenada (backend); FEAT-005: Formato de erro único e não regressão do backend
- RF/RNF: RF-003, RF-008, RNF-001, RNF-003, RNF-004, RNF-009
- BR: BR-003, BR-006, BR-007, BR-009, BR-011, BR-012
- Comportamento: "Condições atuais por coordenada"; "Tradução da condição do tempo"; "Direção do vento por
  extenso"; "Formato único de erro do backend"
- SCN: SCN-014, SCN-015, SCN-016, SCN-017, SCN-018, SCN-019
- TST: TST-008, TST-009
- Impacto contratual: `impacto-contratual/clima/contrato.md` → "Condições atuais por coordenada", "Tradução da
  condição do tempo", "Direção do vento por extenso"

## Limites

- Dentro do escopo:
  - `dtos/WeatherResponse` (record com os 13 campos e tipos da TechSpec; nulos serializados como `null`);
  - `services/WeatherService.current(String latitude, String longitude)`: normalização de extremidades, `parseDouble`,
    rejeição de `NaN`/`Infinity`, faixas inclusivas, `current`/`current.time` ausente → 502, `isDay` 1/0/outro,
    `latitude`/`longitude` ecoadas da requisição;
  - traduções puras `describeWeatherCode(Integer)` (29 entradas + "Condição desconhecida") e
    `windDirection(Double)` (8 pontos, normalização `((g % 360) + 360) % 360`);
  - `controllers/WeatherController` (`@GetMapping("/weather")`, parâmetros `String` com `required = false`).
- Fora do escopo: geolocalização (frontend, task 08); log de coordenadas (proibido); 16 pontos de direção.
- Arquivos esperados:
  - `backend/src/main/java/com/cezar/dev/airquality/dtos/WeatherResponse.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/services/WeatherService.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/controllers/WeatherController.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/services/WeatherServiceTest.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/controllers/WeatherControllerTest.java` (novo)
- Contratos afetados: `GET /weather` (JSON de sucesso e de erro), consumido pela task 06 (`WeatherApi.getWeather`).

## Dependências

- `task_02` — usa `ForecastApiClient` e `ForecastApiResponse`. Independente da task 03.

## Subtasks

- [x] Criar `WeatherResponse`, `WeatherService` e `WeatherController` conforme a TechSpec.
- [x] Testar o service (TST-008) com `ForecastApiClient` mockado:
  - tabela WMO parametrizada com as 29 entradas e os casos `null`, `4`, `100`, `-1`;
  - direção com 0, 22.49, 22.5, 45, 67.49, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5, 337.49, 337.5, 359.99, 360,
    720, -45 (→ Noroeste), `null`, `NaN`;
  - coordenadas ausente, vazia, `abc`, `NaN`, `Infinity`, -90.0001, 90, -180, 180.0001, com `verifyNoInteractions`
    no inválido;
  - mapeamento completo; `current` e `time` ausentes → `UpstreamException`; demais campos ausentes → `null`;
    `isDay` 1/0/2/ausente.
- [x] Testar o controller (TST-009) com `@WebMvcTest(WeatherController.class)` + `@Import(ApiExceptionHandler.class)` +
      `@MockitoBean WeatherService`: `200` com JSON igual ao exemplo da TechSpec e com campos `null` presentes;
      `400 INVALID_COORDINATES`; `502 UPSTREAM_ERROR` sem `reason` do provedor; `504 UPSTREAM_TIMEOUT`.
- [x] Nomear os testes com o ID do cenário (ex.: `@DisplayName("SCN-014 …")`).

## Critérios de sucesso

- [x] `GET /weather` devolve o contrato JSON da TechSpec com condição e direção em PT-BR.
- [x] Cada limite das duas tabelas de tradução tem caso de teste.
- [x] Coordenada inválida responde `400 INVALID_COORDINATES` sem chamar o client.
- [x] Nomes dos testes citam SCN-014 a SCN-019
- [x] `cd backend && ./mvnw test` verde, sem acesso à rede.
- [x] Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd backend && ./mvnw test` com `WeatherServiceTest` e `WeatherControllerTest` verdes.
- Contagem de casos parametrizados (≥ 33 WMO, ≥ 19 direção) no relatório Surefire.

## Evidências produzidas

- `cd backend && ./mvnw test` verde (Surefire): WeatherServiceTest 79 casos (WMO 29+4=33, direção 17 + null/NaN/infinito), WeatherControllerTest 6 casos; sem falhas nem erros.

