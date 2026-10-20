---
name: novo-componente-angular
description: Cria um componente Angular 21 standalone deste projeto (nome.ts/.html/.css/.spec.ts, signals, input()/output(), @if/@for, CSS só com tokens do design system). Use sempre que a tarefa pedir um novo componente de apresentação, card, lista, formulário, skeleton ou mensagem.
---

# Novo componente Angular

Regras aplicáveis: `.claude/rules/frontend.md`, `diretorios.md`, `design-system.md`, `acessibilidade.md`,
`ssr.md`.

## Passos

1. **Nomes** — use exatamente o nome do prompt. `foo-card` → arquivos `foo-card.ts`, `foo-card.html`,
   `foo-card.css` em `src/app/<feature>/`, classe `FooCard`, seletor `app-foo-card`. Sem `.component`.
   (Pode gerar com `npx ng g c <feature>/foo-card --type= --skip-tests` e conferir os nomes, ou criar à mão.)
2. **Classe** (componente de apresentação — não injeta serviço de API):
   ```ts
   import { Component, computed, input, output } from '@angular/core';

   @Component({
     selector: 'app-foo-card',
     imports: [],
     templateUrl: './foo-card.html',
     styleUrl: './foo-card.css',
   })
   export class FooCard {
     readonly data = input.required<Foo>();
     readonly selected = output<Foo>();

     protected readonly label = computed(() =>
       [this.data().name, this.data().region].filter(Boolean).join(', '),
     );
   }
   ```
   - Derivações (rótulos, arredondamento, formatação) em `computed`, não no template.
   - Para focar um elemento ao entrar num estado: `viewChild`/`viewChildren` + `afterNextRender` ou um
     `effect` que chame `.focus()` só no navegador.
3. **Template**:
   - `@if` / `@for (item of items(); track item.id)` / `@switch`;
   - interação sempre em `<button type="button">` (ou `type="submit"` dentro de `<form>`);
   - campos com `<label for>`; erros com `role="alert"`; ícones como texto ASCII (`[+]`, `[x]`).
4. **CSS** (`foo-card.css`):
   - só `var(--…)` dos tokens de `src/styles.css`; se faltar um token, adicione-o em `styles.css` com um valor
     permitido pela regra, nunca no componente;
   - `:host { display: block; }` quando o componente for bloco;
   - responsivo com `@media (max-width: 639px)`;
   - foco: `:focus-visible { outline: 2px solid var(--color-accent); outline-offset: 2px; }`;
   - nada de `box-shadow`, gradiente, `blur`, itálico ou classes Tailwind.
5. **Skeleton** (quando houver): mesmo `min-height`, padding e grade do componente real, blocos com
   `background: var(--color-surface-card)` e:
   ```css
   @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
   .block { animation: pulse 1.2s ease-in-out infinite; }
   @media (prefers-reduced-motion: reduce) { .block { animation: none; } }
   ```
6. **Teste** `foo-card.spec.ts` (Vitest): criar com `TestBed.createComponent`, setar inputs com
   `fixture.componentRef.setInput('data', ...)`, `await fixture.whenStable()`, conferir texto renderizado e
   emissão de `output` (espione com `vi.fn()` via `subscribe`).
7. Use o componente no pai adicionando a classe em `imports` do pai.

## Checklist

- [ ] Standalone, `input()`/`output()`, `inject()` se precisar de algo, sem `NgModule`.
- [ ] Nenhum valor de cor/tamanho/fonte hardcoded no `.css` do componente.
- [ ] Acessível por teclado, foco visível, textos em PT-BR.
- [ ] Nenhum acesso a `window`/`navigator`/`document` fora de guarda de navegador.
