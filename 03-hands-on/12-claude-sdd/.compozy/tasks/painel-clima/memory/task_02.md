# Task Memory: task_02.md

Keep only task-local execution context here. Do not duplicate facts that are obvious from the repository, task file, PRD documents, or git history.

## Objective Snapshot

## Important Decisions

- Geocoding usa `queryParam("name","{name}")` + `encode().buildAndExpand(name)` (nome com `{}` não vira variável de URI).
- Exceções são lançadas só com mensagem fixa (sem causa/corpo); log WARN: API, categoria, duração.

## Learnings

## Files / Surfaces

## Errors / Corrections

## Ready for Next Run

Task concluída; `GeocodingApiClient.search(String)` e `ForecastApiClient.current(double,double)` devolvem os DTOs externos (`results`/`current` podem ser nulos — validar nos services).
