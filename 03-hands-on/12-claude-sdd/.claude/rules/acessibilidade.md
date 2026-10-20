---
paths:
  - "frontend/src/**"
---

# Acessibilidade

- `<html lang="pt-BR">` no `src/index.html`; textos da interface em PT-BR.
- Foco visível em **todo** elemento interativo: `outline: 2px solid var(--color-accent)`,
  `outline-offset: 2px` (use `:focus-visible`). Nunca `outline: none` sem substituto.
- Contraste mínimo WCAG AA (4,5:1) para texto nos dois temas — os pares de tokens de `design-system.md` já
  atendem; não combinar tokens fora desses pares.
- Todo input tem `<label for>` visível.
- Área que recebe resultados assíncronos: `aria-live="polite"` e `aria-busy="true"` enquanto carrega.
- Mensagem de erro com `role="alert"`.
- Elementos clicáveis são `<button>` (ou `<a>` para navegação), nunca `<div>` com `click`; listas
  navegáveis por teclado (Tab + Enter).
- Ao mudar de estado de forma que o usuário precise agir (lista para escolher, erro de validação), mover o
  foco para o elemento relevante (primeiro item, input).
- Skeleton de carregamento: animação de opacidade `1 → 0.5`, `1.2s`, infinita; desativada em
  `@media (prefers-reduced-motion: reduce)`.
- Skeleton tem as mesmas dimensões, padding e grade do componente final (sem salto de layout).
