# Task Memory: task_06.md

## Objective Snapshot
Camada de dados do frontend em `frontend/src/app/weather/` (WeatherApi, BrowserLocation, city-name, weather-messages) + specs.

## Important Decisions
- `toFailure` não conhece a fase; 504/5xx sem `code` → `unavailable`; só `code` do corpo escolhe 404/400/502/504.
- Skills `cy-*` não estavam instaladas; fluxo seguido manualmente.

## Learnings
- `app.html` legado do scaffold contém URLs https (varredura); sai na task 08.

## Files / Surfaces
Quatro arquivos + quatro specs novos em `frontend/src/app/weather/`.

## Errors / Corrections
Nenhum além de um erro de tipo em spec, corrigido.

## Ready for Next Run
Concluída; `npm test` verde (44 testes). Sem commit (auto-commit desligado).
