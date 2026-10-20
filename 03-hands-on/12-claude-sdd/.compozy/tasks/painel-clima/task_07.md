---
status: completed
title: "Task 7.0: Frontend — componentes de apresentação do painel"
type: frontend
complexity: medium
dependencies: [task_05, task_06]
---

# Task 7.0: Frontend — componentes de apresentação do painel

## Rastreabilidade

- FEAT-003: Painel de busca e exibição do clima; FEAT-006: Identidade visual, tema e acessibilidade
- RF/RNF: RF-001, RF-002, RF-003, RF-004, RF-005, RF-006, RF-008, RNF-005, RNF-007, RNF-008
- BR: BR-005, BR-006, BR-007, BR-010, BR-011, BR-012
- Comportamento: "Busca por nome no painel"; "Escolha entre cidades homônimas"; "Exibição das condições atuais";
  "Carregamento, falhas e Tentar novamente"; "Acessibilidade do painel"
- SCN: SCN-025, SCN-026, SCN-027, SCN-028, SCN-029
- TST: TST-015, TST-016, TST-017, TST-018, TST-019
- Impacto contratual: `impacto-contratual/clima/contrato.md` → comportamentos acima (apresentação)

## Limites

- Dentro do escopo (pasta `frontend/src/app/weather/`, componentes standalone com `.ts/.html/.css/.spec.ts`, só
  `input()`/`output()`, `@if`/`@for`, CSS só com `var(--…)`):
  - `CitySearchForm` (`app-city-search-form`): `<label for="city-name">Cidade</label>`, input sem `maxlength`,
    "Buscar" (submit), "Usar minha localização" só com `geolocationAvailable`, mensagem `role="alert"` com
    `aria-describedby`/`aria-invalid`, `focusInput()`, botões nunca desabilitados por `busy`, empilha abaixo de 640px;
  - `CityOptions` (`app-city-options`): título "Encontramos N cidades. Escolha uma:", `<ul>` de `<button>`,
    "Nome — Região, País" + " (Subregião)" só em colisão (PRM-020), `track city.id`, foco no 1º botão;
  - `WeatherCard` (`app-weather-card`): rótulo, badge `[ DIA ]`/`[ NOITE ]` (omitida se nulo), temperatura,
    condição, sensação, umidade, vento `12,7 km/h · Leste`, "Medido às HH:mm (horário local)" via `slice(11, 16)`,
    `Intl.NumberFormat('pt-BR')`, "—" em nulos;
  - `WeatherSkeleton` (`app-weather-skeleton`): mesma grade do cartão, pulso `1 → 0.5` 1.2s, desligado em
    `prefers-reduced-motion: reduce`, `aria-hidden="true"` + texto oculto "Carregando o clima…";
  - `StatusMessage` (`app-status-message`): `error` com `role="alert"` e `[x]` em `--color-danger`; `info` com `[i]`
    em `--color-text-mute`; "Tentar novamente" quando `retryable`, emitindo `retry`.
- Fora do escopo: estado, HTTP, geolocalização e tema (task 08); `new Date`/`DatePipe` (proibidos).
- Arquivos esperados (todos novos, cada um com `.ts`, `.html`, `.css`, `.spec.ts`):
  - `frontend/src/app/weather/city-search-form.*`
  - `frontend/src/app/weather/city-options.*`
  - `frontend/src/app/weather/weather-card.*`
  - `frontend/src/app/weather/weather-skeleton.*`
  - `frontend/src/app/weather/status-message.*`
- Contratos afetados: `input()`/`output()` da tabela "Componentes de apresentação" da TechSpec (consumidos pela
  task 08); tipos `City`/`Weather` e `MESSAGES` da task 06.

## Dependências

- `task_05` — tokens de `styles.css` usados no CSS dos componentes.
- `task_06` — tipos `City`/`Weather` de `weather-api.ts`.

## Subtasks

- [x] Criar os cinco componentes conforme a skill `novo-componente-angular` e a TechSpec.
- [x] TST-015 `city-search-form.spec.ts`: submit pelo botão e por Enter emite o texto bruto; digitar não emite;
      botão de localização ausente com `geolocationAvailable=false`; mensagem com `role="alert"`, `aria-invalid` e
      `aria-describedby`; `focusInput()` foca o campo; botões habilitados com `busy=true`.
- [x] TST-016 `city-options.spec.ts`: título com N; texto "Nome — Região, País"; sufixo de sub-região só em
      colisão; clique emite `selected`; foco no 1º botão.
- [x] TST-017 `weather-card.spec.ts`: valores formatados em PT-BR; "—" em nulos; direção nula mostra só velocidade;
      `16:00` a partir de `"2026-10-20T16:00"`; badge `[ DIA ]`, `[ NOITE ]` e omitida.
- [x] TST-018 `weather-skeleton.spec.ts`: `aria-hidden="true"` e texto oculto "Carregando o clima…".
- [x] TST-019 `status-message.spec.ts`: `role="alert"` e `[x]` em `error`; sem `role="alert"` e `[i]` em `info`;
      botão "Tentar novamente" só com `retryable` e emissão de `retry`.
- [x] Nomear cada `it` com o ID do cenário (ex.: `it('SCN-025 …')`).

## Critérios de sucesso

- Cada componente só recebe `input()` e emite `output()`; nenhum injeta `WeatherApi`, `HttpClient` ou `BrowserLocation`.
- CSS sem cor, sombra, gradiente ou `blur` literal; foco visível com `outline` 2px/offset 2px via tokens;
  `@media (max-width: 639px)` como breakpoint único.
- Nomes dos testes citam SCN-025 a SCN-029
- `cd frontend && npm test` verde.
- Diff restrito aos arquivos esperados.

## Evidências esperadas

- Saída de `cd frontend && npm test` com as cinco specs verdes.
- Varredura de cor/sombra hardcoded e de `outline: none` da skill `validar-entrega` sem ocorrências.

## Evidências produzidas

- `cd frontend && npm test`: 10 arquivos, 67 testes verdes (cinco specs novas, SCN-025 a SCN-029); `npm run build` ok.
- Varredura de cor/sombra/gradiente/blur/`outline: none`/itálico em `weather/*.css`: sem ocorrências.

