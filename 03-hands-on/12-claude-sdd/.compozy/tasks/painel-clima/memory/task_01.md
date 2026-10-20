# Task Memory: task_01.md

Keep only task-local execution context here. Do not duplicate facts that are obvious from the repository, task file, PRD documents, or git history.

## Objective Snapshot

## Important Decisions

## Learnings

## Files / Surfaces

## Errors / Corrections

## Ready for Next Run

## Done (task_01)
- Criados ErrorResponse, 4 exceções, ApiExceptionHandler, 2 testes. `./mvnw test` verde.
- Exceções Upstream*: construtores (message) e (message, cause); CityNotFoundException sem args; handler ignora mensagem da exceção (exceto InvalidRequestException).
- ApiExceptionHandlerTest usa standaloneSetup + controller stub interno (sem @WebMvcTest).
