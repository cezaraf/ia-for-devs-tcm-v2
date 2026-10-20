# Task Memory: task_08.md

Keep only task-local execution context here. Do not duplicate facts that are obvious from the repository, task file, PRD documents, or git history.

## Objective Snapshot

## Important Decisions

## Learnings

## Files / Surfaces

## Errors / Corrections

## Ready for Next Run

## Resumo da execução
- `WeatherPanel` (ts/html/css/spec) + `App` só com `<app-weather-panel />`; `npm test` 94 verdes; `npm run build` ok.
- TST-022: HTML pré-renderizado tem "Painel de clima" e `data-theme="day"`, sem "Usar minha localização" nem cartão.
- Decisão: `run(op)` devolve `Outcome` (weather/cities/failure+op); falhas viram valores no `catchError` interno, então o `Subject` nunca morre. Lista vazia de `searchCities` → `not-found`. `invalid` não renderiza StatusMessage (mensagem fica no form).
- Spec anexa o fixture ao `document.body` para asserir foco real.
- Skills `cy-*` não instaladas; fluxo manual. Sem commit.
