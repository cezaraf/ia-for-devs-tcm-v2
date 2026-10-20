---
status: completed
title: "Task 3.0: Backend — endpoint GET /cities"
type: backend
complexity: medium
dependencies: [task_02]
---

# Task 3.0: Backend — endpoint GET /cities

## Rastreabilidade

- FEAT-001: Busca de cidades por nome (backend); FEAT-005: Formato de erro único e não regressão do backend
- RF/RNF: RF-001, RF-002, RF-004, RNF-001, RNF-004, RNF-009
- BR: BR-001, BR-002, BR-003, BR-006
- Comportamento: "Busca de cidades por nome"; "Formato único de erro do backend"
- SCN: SCN-008, SCN-009, SCN-010, SCN-011, SCN-012, SCN-013
- TST: TST-006, TST-007
- Impacto contratual: `impacto-contratual/clima/contrato.md` → "Busca de cidades por nome"

## Limites

- Dentro do escopo:
  - `dtos/CityResponse` (`record CityResponse(long id, String name, String region, String subregion,
    String country, String countryCode, double latitude, double longitude, String timezone)`);
  - `services/CityService.search(String name)`: normalização pelo conjunto explícito `White_Space` + U+FEFF,
    contagem por code points (2 a 100), filtro `feature_code` `PPL*` ou ausente, 404 para lista vazia, corte em
    10 mantendo a ordem, 502 para item sem `id`/`name`/`latitude`/`longitude`, mapeamento `admin1` → `region`,
    `admin2` → `subregion` (PRM-020);
  - `controllers/CityController` (`@GetMapping("/cities")`, `name` como `String` com `required = false`).
- Fora do escopo: auto-seleção de capital (PRM-021, rejeitada pelo dev em 2026-10-20); cache; `GET /weather` (task 04).
- Arquivos esperados:
  - `backend/src/main/java/com/cezar/dev/airquality/dtos/CityResponse.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/services/CityService.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/controllers/CityController.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/services/CityServiceTest.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/controllers/CityControllerTest.java` (novo)
- Contratos afetados: `GET /cities` (JSON de sucesso e de erro), consumido pela task 06 (`WeatherApi.searchCities`).

## Dependências

- `task_02` — usa `GeocodingApiClient` e `GeocodingApiResponse` (e, por transitividade, as exceções da task 01).

## Subtasks

- [x] Criar `CityResponse`, `CityService` e `CityController` conforme a TechSpec (controller fino, regra no service).
- [x] Validar antes de qualquer HTTP externo: `null`, `""`, `"  "`, `" a "`, `"\u00A0a\u00A0"` inválidos;
      `"\uFEFFab\u2007"` → `"ab"` válido; 2 e 100 code points válidos; 101 inválido; emoji contado por code point.
- [x] Testar o service (TST-006) com `GeocodingApiClient` mockado: matriz de validação acima com
      `verifyNoInteractions` no inválido; `results` ausente/vazio → `CityNotFoundException`; só `AIRP`/`ISL` → 404;
      mistura PPL/AIRP mantém só PPL* e a ordem; `feature_code` ausente mantido; 12 itens → 10; item sem `name`
      → `UpstreamException`; mapeamento `admin1`/`admin2`/`country_code`/`timezone`.
- [x] Testar o controller (TST-007) com `@WebMvcTest(CityController.class)` + `@Import(ApiExceptionHandler.class)` +
      `@MockitoBean CityService`: `200` com campos `null` presentes no JSON; `400 INVALID_CITY_NAME`;
      `404 CITY_NOT_FOUND`; `502 UPSTREAM_ERROR`; `504 UPSTREAM_TIMEOUT`; `$.code`/`$.message` exatos e ausência de
      outros campos no corpo de erro.
- [x] Nomear os testes com o ID do cenário (ex.: `@DisplayName("SCN-008 …")`).

## Critérios de sucesso

- `GET /cities?name=São José` (mock) devolve 1 a 10 `CityResponse` na ordem do provedor, sem aeroportos e ilhas.
- Entrada inválida responde `400 INVALID_CITY_NAME` sem chamar o client.
- Cada status do contrato (200/400/404/502/504) tem teste de controller com corpo exato.
- Nomes dos testes citam SCN-008 a SCN-013
- `cd backend && ./mvnw test` verde, sem acesso à rede.
- Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd backend && ./mvnw test` com `CityServiceTest` e `CityControllerTest` verdes.
- Payloads JSON de sucesso e de erro conferidos nos testes de controller (citados no relatório da task).

## Evidências produzidas

- `cd backend && ./mvnw test`: 49 testes, 0 falhas (`CityServiceTest` 21, `CityControllerTest` 6, sem rede).
- `CityControllerTest` confere JSON de 200 (campos `null` presentes) e corpos exatos de 400/404/502/504 (`$.length()==2`).

