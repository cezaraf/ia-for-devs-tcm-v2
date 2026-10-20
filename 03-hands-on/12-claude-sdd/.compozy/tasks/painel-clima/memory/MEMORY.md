# Workflow Memory

Keep only durable, cross-task context here. Do not duplicate facts that are obvious from the repository, PRD documents, or git history.

## Current State

## Shared Decisions

## Shared Learnings

## Open Risks

## Handoffs

- task_01: `InvalidRequestException(code, message)` carrega code/mensagem; demais exceções têm mensagens fixas no `ApiExceptionHandler`. Tasks 02–04 só lançam as exceções.
- task_03: `CityService.search(name)` já normaliza (White_Space+FEFF), valida 2–100 code points e lança `InvalidRequestException("INVALID_CITY_NAME", "Informe o nome de uma cidade com 2 a 100 caracteres.")`; task 04 pode seguir o mesmo padrão (controller fino, `String` opcional).
- task_04: `WeatherService.current(String,String)` ecoa lat/lon da requisição, lança `InvalidRequestException("INVALID_COORDINATES", ...)` antes de chamar o client; `GET /weather` devolve 13 campos com nulls presentes.
- task_06: `toFailure(error)` → `{state, kind, text, retryable}` (sem fase): `TimeoutError` sempre vira `timeout`; o orquestrador (task 08) deve trocar para `geo-unavailable` se ocorrer antes da posição. Também exporta `MESSAGES`, `LOADING_TEXT`, `LOCATION_LABEL`, `RETRY_LABEL`, `choosingTitle(n)`; `BrowserLocation.getPosition()` erra com `{code}`; `City/Weather/ApiError` em `weather-api.ts`.
