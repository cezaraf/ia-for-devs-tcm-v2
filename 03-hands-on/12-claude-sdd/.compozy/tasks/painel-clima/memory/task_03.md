# Task Memory: task_03.md

Keep only task-local execution context here. Do not duplicate facts that are obvious from the repository, task file, PRD documents, or git history.

## Objective Snapshot

## Important Decisions

## Learnings

## Files / Surfaces

## Errors / Corrections

## Ready for Next Run

## Notes (task_03 concluída)
- Criados: `CityResponse`, `CityService`, `CityController` + `CityServiceTest` (21), `CityControllerTest` (6). `./mvnw test` verde (49).
- `services/` não existia em `main`; foi criado. Skills `cy-*` não instaladas: fluxo seguido à mão.
- Armadilha de teste: reconfigurar mock que já lança exceção exige `doThrow(...).when(mock)`, não `when(mock.x())`.
- Service valida/normaliza antes do client e envia o valor normalizado; `results` nulo/vazio/pós-filtro vazio → 404; filtro `PPL*` ou ausente; item sem id/name/lat/lon → `UpstreamException`.
