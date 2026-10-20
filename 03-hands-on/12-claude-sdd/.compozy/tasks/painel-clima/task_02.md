---
status: completed
title: "Task 2.0: Backend — clients Open-Meteo (Geocoding e Forecast)"
type: backend
complexity: medium
dependencies: [task_01]
---

# Task 2.0: Backend — clients Open-Meteo (Geocoding e Forecast)

## Rastreabilidade

- FEAT-001: Busca de cidades por nome (backend); FEAT-002: Condições atuais por coordenada (backend);
  FEAT-005: Formato de erro único e não regressão do backend
- RF/RNF: RF-001, RF-003, RNF-002, RNF-004, RNF-009, RNF-011
- BR: BR-003, BR-006, BR-011, BR-012
- Comportamento: "Busca de cidades por nome"; "Condições atuais por coordenada"; "Formato único de erro do backend"
- SCN: SCN-003, SCN-004, SCN-005, SCN-006, SCN-007
- TST: TST-003, TST-004, TST-005
- Impacto contratual: `impacto-contratual/clima/contrato.md` → os três comportamentos acima (integração)

## Limites

- Dentro do escopo:
  - chaves de `application.properties` com os valores exatos da TechSpec (`geocoding-api.*`, `forecast-api.*`:
    `base-url`, `connect-timeout=2s`, `read-timeout=4s`);
  - `clients/OpenMeteoConfig` com os beans `geocodingApiRestClient` e `forecastApiRestClient`, cada um com a
    própria `SimpleClientHttpRequestFactory`;
  - DTOs do JSON externo `GeocodingApiResponse` e `ForecastApiResponse` (records aninhados, wrappers,
    `@JsonIgnoreProperties(ignoreUnknown = true)`, anotações `com.fasterxml.jackson.annotation`);
  - `GeocodingApiClient.search(String name)` e `ForecastApiClient.current(double latitude, double longitude)`,
    URIs por `UriComponentsBuilder ... encode()`, tradução de falhas e log `WARN` sem query, coordenadas ou corpo.
- Fora do escopo: validação de entrada, filtro `feature_code`, mapeamento para DTOs de resposta e traduções
  (tasks 03 e 04); retry, cache ou circuit breaker (ADR-002).
- Arquivos esperados:
  - `backend/src/main/resources/application.properties` (alterado: novas chaves)
  - `backend/src/main/java/com/cezar/dev/airquality/clients/OpenMeteoConfig.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/clients/GeocodingApiClient.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/clients/ForecastApiClient.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/dtos/GeocodingApiResponse.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/dtos/ForecastApiResponse.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/clients/GeocodingApiClientTest.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/clients/ForecastApiClientTest.java` (novo)
- Contratos afetados: integração de saída com a Open-Meteo (não exposta ao navegador); exceções
  `UpstreamException`/`UpstreamTimeoutException` da task 01.

## Dependências

- `task_01` — usa `UpstreamException` e `UpstreamTimeoutException`.

## Subtasks

- [x] Adicionar as chaves de `application.properties` com os valores exatos da TechSpec.
- [x] Criar `OpenMeteoConfig` e os dois clients injetando o bean por `@Qualifier` e a base URL por `@Value`.
- [x] Montar as requisições exatas: Geocoding `/search?name=…&count=10&language=pt&format=json`; Forecast
      `/forecast?latitude=…&longitude=…&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m&timezone=auto&temperature_unit=celsius&wind_speed_unit=kmh`.
- [x] Traduzir falhas como na skill `cliente-api-externa`: `RestClientResponseException` e corpo `null` ou
      inválido → `UpstreamException`; `ResourceAccessException` com `SocketTimeoutException`/`HttpTimeoutException`
      na cadeia → `UpstreamTimeoutException`, senão `UpstreamException`.
- [x] Testar com `MockRestServiceServer.bindTo(RestClient.builder())` (sem rede real, sem `requestFactory(...)`
      depois do `bindTo`): URL e query exatas, "São José" codificado uma única vez, campos desconhecidos aceitos,
      `{"generationtime_ms":0.1}` → `results` nulo, `withServerError()`, `withStatus(BAD_REQUEST)` com
      `{"error":true,"reason":"…"}`, corpo `"not json"`, `SocketTimeoutException`, `ConnectException`.
- [x] TST-005: garantir que `AirqualityApplicationTests.contextLoads` continue verde com os dois beans e as properties.
- [x] Nomear os testes com o ID do cenário (ex.: `@DisplayName("SCN-003 …")`).

## Critérios de sucesso

- Cada client produz exatamente a URL da TechSpec e só ele faz HTTP para a sua API.
- Falhas do provedor viram `UpstreamException`; timeouts de conexão e leitura viram `UpstreamTimeoutException`.
- Nenhum log contém query string, coordenada, nome de cidade, corpo externo ou stack trace completo.
- Nomes dos testes citam SCN-003 a SCN-007
- `cd backend && ./mvnw test` verde, sem acesso à rede.
- Diff restrito aos arquivos esperados; `pom.xml` sem dependência nova.

## Evidências esperadas

- Saída de `cd backend && ./mvnw test` com `GeocodingApiClientTest`, `ForecastApiClientTest` e
  `AirqualityApplicationTests` verdes.
- `git diff --stat -- backend/pom.xml` vazio.

## Evidências produzidas

- `./mvnw test` verde: AirqualityApplicationTests 1, ForecastApiClientTest 7, GeocodingApiClientTest 9, ApiExceptionHandlerTest 4, HealthCheckControllerTest 1; sem rede.
- `git diff --stat -- backend/pom.xml` vazio.

