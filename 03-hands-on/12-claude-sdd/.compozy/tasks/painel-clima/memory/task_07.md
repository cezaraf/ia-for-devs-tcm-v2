# Task Memory: task_07.md

Keep only task-local execution context here. Do not duplicate facts that are obvious from the repository, task file, PRD documents, or git history.

## Objective Snapshot

## Important Decisions

## Learnings

## Files / Surfaces

## Errors / Corrections

## Ready for Next Run

## Resumo da execução
- 5 componentes em `frontend/src/app/weather/` (ts/html/css/spec); `npm test` 67 verdes; build ok; varredura de cor/sombra/outline limpa.
- `min-height: 240px` no WeatherCard e no WeatherSkeleton (mesmo valor, literal, não há token); skeleton usa larguras % nos blocos.
- Specs de card leem `.row dt/dd` (o compilador remove espaços entre elementos; textContent corrido não serve).
- Submit do form lê `value` do input via `viewChild` (texto bruto); testes disparam `submit` no form.
