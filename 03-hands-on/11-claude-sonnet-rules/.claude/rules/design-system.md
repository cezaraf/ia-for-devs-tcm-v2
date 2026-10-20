---
paths:
  - "frontend/src/**/*.css"
  - "frontend/src/**/*.html"
  - "frontend/src/**/*.ts"
---

# Design system — painel terminal-like

Geração determinística: use **exatamente** os valores abaixo. Não inventar cor, tamanho, espaçamento, sombra
ou fonte fora destes tokens.

## Tokens (todos em `frontend/src/styles.css`)

- Todos os tokens (cor, raio, fonte, espaçamento) são variáveis CSS em `src/styles.css`. Componentes usam só
  `var(--…)` no seu próprio `.css`; nunca valores hardcoded no componente.
- Cores definidas por `[data-theme="day"]` e `[data-theme="night"]` (o tema é aplicado por atributo
  `data-theme` no elemento raiz da página/painel). Nunca por `prefers-color-scheme`.

| Token                    | `day` (claro)          | `night` (escuro)             | Uso                                   |
|--------------------------|------------------------|------------------------------|---------------------------------------|
| `--color-bg`             | `#fdfcfc`              | `#201d1d`                    | fundo da página                       |
| `--color-surface`        | `#f8f7f7`              | `#302c2c`                    | input, hover de linha (night: card)   |
| `--color-surface-card`   | `#f1eeee`              | `#302c2c`                    | skeleton, botão desabilitado          |
| `--color-text`           | `#201d1d`              | `#fdfcfc`                    | texto principal                       |
| `--color-text-body`      | `#424245`              | `#9a9898`                    | texto de corpo                        |
| `--color-text-mute`      | `#646262`              | `#9a9898`                    | texto secundário                      |
| `--color-hairline`       | `rgba(15,0,0,0.12)`    | `rgba(253,252,252,0.12)`     | bordas e separadores                  |
| `--color-hairline-strong`| `#646262`              | `#9a9898`                    | borda do botão secundário             |
| `--color-accent`         | `#201d1d`              | `#fdfcfc`                    | ver uso restrito abaixo               |
| `--color-on-accent`      | `#fdfcfc`              | `#201d1d`                    | texto sobre acento                    |
| `--color-danger`         | `#ff3b30`              | `#ff3b30`                    | só o marcador `[x]`                   |

- `--radius-control: 4px` (input, botões, badge); `--radius-container: 0px` (cards, skeleton, listas,
  mensagens de erro).
- `--font-mono: 'JetBrains Mono', 'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
  'Liberation Mono', 'Courier New', monospace`.
- Escala de espaçamento (únicos valores permitidos): 4, 8, 12, 16, 24, 32, 48, 96 px — expostos como
  variáveis (ex. `--space-4` … `--space-96`).

## Uso de cor

- Acento (ink/cream) **só** em: preenchimento do botão primário, contorno de foco, item de lista com
  foco/hover.
- Nunca usar o azul `#007aff` nem cores de warning/success.
- `--color-danger` aparece **somente** no marcador `[x]`; o texto da mensagem de erro usa `--color-text`.
- Troca de tema sem transição animada.

## Tipografia

- Somente JetBrains Mono (pesos 400/500/700), carregada por `<link>` do Google Fonts no `src/index.html`
  com `display=swap`. Sem itálico, sem fonte proporcional.
- Peso 700: títulos e valores de maior destaque. Peso 500: botões, badges, item selecionado, textos de
  ênfase. Peso 400: corpo, input, listas, linhas de detalhe, mensagens, rodapés.
- Tamanhos: texto padrão 16px (títulos, corpo, input, botões); textos auxiliares 14px (badge, label de campo,
  rodapé) com `line-height: 2`; botões com `line-height: 2`; demais `line-height: 1.5`; `letter-spacing: 0`.

## Layout e controles

- Container: `max-width: 640px`, centralizado, padding lateral 16px, padding superior 96px (48px abaixo
  de 640px), padding inferior 48px. Abaixo de 640px ocupa 100% menos o gutter; sem scroll horizontal.
- Breakpoint único: `@media (max-width: 639px)`.
- Input: altura 40px, padding `8px 12px`, borda 1px `--color-hairline`, fundo `--color-surface`; em foco,
  fundo `--color-bg` e borda 1px `--color-accent`.
- Botões primário e secundário: altura 40px, padding `4px 20px`, 16px peso 500. Primário: fundo
  `--color-accent`, texto `--color-on-accent`. Secundário: fundo transparente, borda 1px
  `--color-hairline-strong`, texto `--color-text`. Desabilitado: fundo `--color-surface-card`.
- Formulário: campo + botões em linha com gap 8px; abaixo de 640px empilha (cada um 100%, gap 8px).
- Linhas de lista: altura 40px, padding `8px 12px`, separador 1px `--color-hairline`; hover/foco com acento.
- Sombra: nenhuma — sem `box-shadow`, gradiente ou `blur`.

## Ícones e bibliotecas

- Ícones são exclusivamente marcadores ASCII em texto (ex. `[+]`, `[x]`, `[ DIA ]`, `[ NOITE ]`).
- Nenhuma biblioteca de UI, ícones ou fontes além da JetBrains Mono.
- Tailwind: manter apenas o `@import 'tailwindcss'` existente em `styles.css`; não usar classes utilitárias
  nem `@theme`.
