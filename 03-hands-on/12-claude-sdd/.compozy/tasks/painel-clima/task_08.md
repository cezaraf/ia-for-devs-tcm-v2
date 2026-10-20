---
status: completed
title: "Task 8.0: Frontend — orquestrador WeatherPanel e página inicial"
type: frontend
complexity: high
dependencies: [task_06, task_07]
---

# Task 8.0: Frontend — orquestrador WeatherPanel e página inicial

## Rastreabilidade

- FEAT-003: Painel de busca e exibição do clima; FEAT-004: Clima pela localização do navegador;
  FEAT-006: Identidade visual, tema e acessibilidade
- RF/RNF: RF-001, RF-002, RF-004, RF-005, RF-006, RF-007, RF-008, RF-009, RF-010, RNF-002, RNF-003, RNF-005,
  RNF-006, RNF-009
- BR: BR-002, BR-004, BR-005, BR-008, BR-009, BR-010
- Comportamento: "Busca por nome no painel"; "Escolha entre cidades homônimas"; "Carregamento, falhas e Tentar
  novamente"; "Operação mais recente prevalece"; "Clima pela localização do navegador"; "Tema dia/noite";
  "Acessibilidade do painel"; "Renderização no servidor sem efeitos do navegador"
- SCN: SCN-030, SCN-031, SCN-032, SCN-033, SCN-034, SCN-035, SCN-036, SCN-037, SCN-038, SCN-039, SCN-040,
  SCN-041, SCN-042, SCN-043, SCN-044
- TST: TST-020, TST-021, TST-022
- Impacto contratual: `impacto-contratual/clima/contrato.md` → comportamentos acima (orquestração)

## Limites

- Dentro do escopo:
  - `weather-panel.ts|html|css|spec.ts` → `WeatherPanel` (`app-weather-panel`): signals `state`, `weather`,
    `locationLabel`, `cities`, `theme`, `geolocationAvailable`, `lastFailed`; computeds `busy` e `failure`;
    `Subject<Operation>` + `switchMap` + `timeout(15_000)` + `catchError`; operação `cancel` para submit inválido;
    `locate` com flag `positionReceived`; arredondamento `Math.round(v * 100) / 100`; tabela de transições da
    TechSpec; `afterNextRender` só para `geolocationAvailable`; foco programático só no navegador;
    raiz `<main class="panel" [attr.data-theme]>`, `<h1>Painel de clima</h1>`, área
    `<section aria-live="polite" [attr.aria-busy]>` com `@switch`;
  - `app.ts`/`app.html`/`app.css`: `App` renderiza só `<app-weather-panel />` (sai o placeholder e o
    `RouterOutlet` do template; `provideRouter` e `app.routes.ts` permanecem);
  - `app.spec.ts`: passa a verificar `app-weather-panel` (com `provideHttpClient()` + `provideHttpClientTesting()`).
- Fora do escopo: auto-seleção de capital (PRM-021, rejeitada pelo dev em 2026-10-20); persistência de qualquer estado; rotas novas;
  arquivos SSR intocáveis (`server.ts`, `main.server.ts`, `app.config.server.ts`, `app.routes.server.ts`).
- Arquivos esperados:
  - `frontend/src/app/weather/weather-panel.ts`, `.html`, `.css`, `.spec.ts` (novos)
  - `frontend/src/app/app.ts`, `app.html`, `app.css`, `app.spec.ts` (alterados)
- Contratos afetados: estados, mensagens e retry de RF-005/RF-006; consumo dos `input()`/`output()` da task 07 e dos
  serviços da task 06.

## Dependências

- `task_06` — `WeatherApi`, `BrowserLocation`, `city-name.ts`, `weather-messages.ts`.
- `task_07` — os cinco componentes de apresentação (e, por transitividade, os tokens da task 05).
- Não depende do código do backend: os testes mockam `WeatherApi`; a integração real é provada no gate de
  validação e no QA.

## Subtasks

- [x] Implementar `WeatherPanel` e trocar o conteúdo de `app.html`.
- [x] TST-020 `weather-panel.spec.ts`, com `WeatherApi` e `BrowserLocation` mockados (`vi.fn()` com `of`,
      `throwError`, `Subject`) e `vi.useFakeTimers()`:
  - estado inicial `idle`, tema `day`, sem chamada a `WeatherApi` nem a `getPosition` na carga;
  - submit inválido: `invalid`, nenhuma requisição, foco no input, resposta tardia de operação anterior ignorada;
  - 1 cidade encadeia `getWeather` → `success` com rótulo "Nome, Região, País";
  - 2..10 → `choosing`; escolha → `success` com o rótulo da cidade escolhida;
  - 404 → `not-found` sem retry; 502/5xx/`status 0` → `unavailable` com retry; 504 → `timeout` com retry;
  - `INVALID_COORDINATES` → `unavailable` sem botão de retry;
  - retry repete a mesma operação para busca, escolha e localização;
  - resposta antiga ignorada (dois `Subject`s, o primeiro emitido depois);
  - `vi.advanceTimersByTime(15_000)` → `timeout`; `locate` com posição pendente aos 15 s → `geo-unavailable` com retry;
    posição obtida e clima pendente aos 15 s → `timeout`;
  - localização: `getPosition` só após clique, coordenadas arredondadas passadas a `getWeather`, rótulo
    "Sua localização"; `code 1` → `geo-denied` sem retry; `code 2/3` → `geo-unavailable` com retry;
  - `data-theme` `day`/`night` conforme `isDay`, `day` com `isDay` nulo, permanência após erro e na lista;
  - `aria-busy="true"` durante `loading`, esqueleto visível, foco no 1º item em `choosing`.
- [x] TST-021 `app.spec.ts`: `App` renderiza `app-weather-panel`.
- [x] TST-022 `npm run build`: prerender sem erro; HTML pré-renderizado com `Painel de clima`, `data-theme="day"`,
      sem o botão "Usar minha localização" e sem cartão de clima; comando iniciado por `echo SCN-030 SCN-044 TST-022` para que a saída carregue os IDs
- [x] Nomear cada `it` com o ID do cenário (ex.: `it('SCN-030 …')`).

## Critérios de sucesso

- Cada linha da tabela de transições da TechSpec tem ao menos um teste.
- Nenhuma operação permanece em `loading` após 15 s simulados.
- Nenhuma chamada a `navigator`, `WeatherApi` ou `getPosition` no construtor, em inicializador de campo ou no
  servidor; coordenada nunca em `console`, `localStorage` ou signal fora da operação.
- Nomes dos testes citam SCN-030 a SCN-044
- `cd frontend && npm test` e `cd frontend && npm run build` verdes.
- Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd frontend && npm test` com `weather-panel.spec.ts` e `app.spec.ts` verdes.
- Saída de `cd frontend && npm run build` e o trecho do HTML pré-renderizado conferido em TST-022
- Varredura de acesso ao navegador sem guarda (skill `validar-entrega`) revisada.

## Evidências produzidas

