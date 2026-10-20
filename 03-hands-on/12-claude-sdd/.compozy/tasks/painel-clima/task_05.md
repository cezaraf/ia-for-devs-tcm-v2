---
status: completed
title: "Task 5.0: Frontend — base da página, tokens e HttpClient"
type: frontend
complexity: low
dependencies: []
---

# Task 5.0: Frontend — base da página, tokens e HttpClient

## Rastreabilidade

- FEAT-006: Identidade visual, tema e acessibilidade
- RF/RNF: RF-010, RNF-001, RNF-005, RNF-006, RNF-008, RNF-011
- BR: BR-003
- Comportamento: "Acessibilidade do painel"; "Tema dia/noite"; "Renderização no servidor sem efeitos do navegador"
- SCN: SCN-020
- TST: TST-010
- Impacto contratual: `impacto-contratual/clima/contrato.md` → "Acessibilidade do painel" (idioma, fonte),
  "Tema dia/noite" (tokens dos dois temas)

## Limites

- Dentro do escopo:
  - `index.html`: `lang="pt-BR"`, `<title>Painel de clima</title>`, `preconnect` e `<link>` da JetBrains Mono
    (pesos 400/500/700, `display=swap`), sem tocar `<base href>`;
  - `styles.css`: `@import 'tailwindcss';` como primeira regra; tokens de cor em `:root, [data-theme='day']` e
    `[data-theme='night']` com os valores exatos de `design-system.md`; raios, fonte, espaçamentos e auxiliares
    nomeados da TechSpec; base de `html, body`; classe `.visually-hidden`;
  - `app.config.ts`: `provideHttpClient(withFetch())`, mantendo `provideClientHydration(withEventReplay())`.
- Fora do escopo: componentes (tasks 07 e 08); `app.html`/`app.spec.ts` (task 08); `angular.json`, `server.ts`,
  `main.server.ts`, `app.config.server.ts`, `app.routes.server.ts` (intocáveis).
- Arquivos esperados:
  - `frontend/src/index.html` (alterado)
  - `frontend/src/styles.css` (alterado)
  - `frontend/src/app/app.config.ts` (alterado)
- Contratos afetados: nenhum contrato entre camadas; tokens consumidos pelas tasks 07 e 08.

## Dependências

- Nenhuma. Independente do backend: depende só dos valores fixados na TechSpec e em `design-system.md`.

## Subtasks

- [x] Ajustar `index.html` (idioma, título, fonte única permitida).
- [x] Definir em `styles.css` os tokens exatos dos dois temas e os auxiliares nomeados; nenhuma classe utilitária
      nem `@theme`; nenhum `prefers-color-scheme`.
- [x] Adicionar `provideHttpClient(withFetch())` em `app.config.ts`.
- [x] Rodar a verificação automatizada TST-010 (build + conferência do HTML pré-renderizado + varredura de
      `styles.css`), registrando a saída com o rótulo `SCN-020`.
- [x] Garantir `npm test` ainda verde (o `app.spec.ts` atual não muda nesta task).

## Critérios de sucesso

- HTML pré-renderizado em `frontend/dist/` contém `lang="pt-BR"`, `<title>Painel de clima</title>` e o `<link>` da
  JetBrains Mono como único recurso externo.
- `styles.css` define `--color-bg` … `--color-danger` para `day` e `night` com os valores de `design-system.md`,
  `--space-4` … `--space-96`, `--radius-control`, `--radius-container` e `--font-mono`.
- `grep -nE "@theme|@apply|prefers-color-scheme" frontend/src` sem ocorrências.
- `cd frontend && npm test` e `cd frontend && npm run build` verdes.
- Diff restrito aos arquivos esperados; `package.json` sem dependência nova.

## Evidências esperadas

- TST-010 (comando, rótulo SCN-020):
  `echo SCN-020 TST-010 && cd frontend && npm run build && grep -rl 'lang="pt-BR"' dist && grep -rl '<title>Painel de clima</title>' dist`
  e `grep -c "data-theme='night'" src/styles.css`.
- Saída de `npm test` verde.

## Evidências produzidas

