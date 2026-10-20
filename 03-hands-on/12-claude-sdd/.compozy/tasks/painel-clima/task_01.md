---
status: completed
title: "Task 1.0: Backend — fundação de erros e não regressão do healthcheck"
type: backend
complexity: low
dependencies: []
---

# Task 1.0: Backend — fundação de erros e não regressão do healthcheck

## Rastreabilidade

- FEAT-005: Formato de erro único e não regressão do backend
- RF/RNF: RNF-004, RNF-010, RF-002 (código `INVALID_CITY_NAME`), RF-005 (códigos que alimentam os estados)
- BR: BR-006
- Comportamento: "Formato único de erro do backend"; preservado: `GET /healthcheck` (sem impacto contratual)
- SCN: SCN-001, SCN-002
- TST: TST-001, TST-002
- Impacto contratual: `sdd/incrementos/painel-clima/impacto-contratual/clima/contrato.md` → "Formato único de erro do backend"

## Limites

- Dentro do escopo:
  - `dtos/ErrorResponse` (`record ErrorResponse(String code, String message)`);
  - exceções unchecked em `exceptions/`: `InvalidRequestException(code, message)`,
    `CityNotFoundException`, `UpstreamException`, `UpstreamTimeoutException`;
  - `controllers/ApiExceptionHandler` (único `@RestControllerAdvice`) com a tabela de status/`code`/mensagem
    exata da TechSpec (seção Backend → Erros);
  - teste do `HealthCheckController` existente.
- Fora do escopo: controllers/services/clients de `/cities` e `/weather` (tasks 02–04); handler genérico
  `@ExceptionHandler(Exception.class)` (proibido pela TechSpec); qualquer mudança em `HealthCheckController`.
- Arquivos esperados:
  - `backend/src/main/java/com/cezar/dev/airquality/dtos/ErrorResponse.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/exceptions/InvalidRequestException.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/exceptions/CityNotFoundException.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/exceptions/UpstreamException.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/exceptions/UpstreamTimeoutException.java` (novo)
  - `backend/src/main/java/com/cezar/dev/airquality/controllers/ApiExceptionHandler.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/controllers/ApiExceptionHandlerTest.java` (novo)
  - `backend/src/test/java/com/cezar/dev/airquality/controllers/HealthCheckControllerTest.java` (novo)
- Contratos afetados: corpo de erro `{"code","message"}` de `GET /cities` e `GET /weather` (consumido pelas
  tasks 03, 04 e 06); `GET /healthcheck` preservado.

## Dependências

- Nenhuma.

## Subtasks

- [x] Criar `ErrorResponse` e as quatro exceções próprias (sem stack trace nem mensagem Java no corpo).
- [x] Criar `ApiExceptionHandler` mapeando: `InvalidRequestException` → 400 com o `code`/mensagem da exceção;
      `CityNotFoundException` → 404 `CITY_NOT_FOUND`; `UpstreamException` → 502 `UPSTREAM_ERROR`;
      `UpstreamTimeoutException` → 504 `UPSTREAM_TIMEOUT`; mensagens PT-BR exatas da TechSpec.
- [x] Testar o handler (TST-002): cada exceção → status, `code`, `message` exatos e corpo só com esses dois
      campos; mensagem interna da causa nunca aparece no corpo.
- [x] Testar `GET /healthcheck` com `@WebMvcTest(HealthCheckController.class)` → `200` `"OK"` (TST-001).
- [x] Nomear os testes com o ID do cenário (`@DisplayName("SCN-001 …")`, `@DisplayName("SCN-002 …")`).

## Critérios de sucesso

- `GET /healthcheck` responde `200` com corpo `OK`, igual à linha de base.
- Cada exceção própria vira o status e o `ErrorResponse` exatos da tabela de erros da TechSpec.
- Nenhum corpo de erro contém nome de classe Java, stack trace ou texto da causa.
- Nomes dos testes citam SCN-001 e SCN-002
- `cd backend && ./mvnw test` verde (inclui `AirqualityApplicationTests.contextLoads`).
- Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd backend && ./mvnw test` com `HealthCheckControllerTest` e `ApiExceptionHandlerTest` verdes.
- Trecho do relatório Surefire com os nomes `SCN-001`/`SCN-002`.

## Evidências produzidas

- `cd backend && ./mvnw test`: verde (6 testes: contextLoads, HealthCheckControllerTest SCN-001, 4× ApiExceptionHandlerTest SCN-002).
