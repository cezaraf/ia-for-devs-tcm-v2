---
paths:
  - "frontend/**"
---

# Frontend — Angular 21

- Componentes **standalone** (sem `NgModule`), com `imports` explícitos no decorator.
- Estado com `signal` / `computed`; entradas com `input()` / `input.required()`, saídas com `output()`;
  dependências com `inject()` (sem injeção por construtor).
- Templates com controle de fluxo `@if` / `@for` (com `track`) / `@switch`; nunca `*ngIf` / `*ngFor`.
- Cada componente tem `templateUrl` e `styleUrl` próprios (`nome.html`, `nome.css`); sem template/estilo inline.
- Nomes de arquivo, classe e seletor conforme `diretorios.md`.
- HTTP:
  - `provideHttpClient(withFetch())` em `app.config.ts`;
  - um único serviço `@Injectable({ providedIn: 'root' })` por funcionalidade faz as chamadas, e só ao backend;
  - URLs relativas sem barra inicial (`api/<recurso>`); parâmetros via `HttpParams`/`params`, nunca
    concatenados à mão;
  - tipar as respostas com `interface`/`type` espelhando o contrato do backend.
- Orquestração de estado:
  - o componente orquestrador modela o estado como união de strings
    (ex. `'idle' | 'loading' | 'success' | 'error'`) em um `signal`;
  - componentes de apresentação não injetam o serviço de API nem mantêm estado de negócio;
  - **nova requisição cancela a anterior** (`switchMap` sobre um `Subject`, ou guardar e cancelar a
    `Subscription`/token da requisição em curso); resposta atrasada de requisição antiga nunca altera o estado;
  - ação que falhou de forma repetível é guardada para que "tentar novamente" a repita exatamente.
- Validação de entrada que pode ser feita no cliente acontece antes de chamar o backend.
- Tudo o que toca `window`, `navigator`, `document` ou HTTP segue `ssr.md`.
- Formatação Prettier do projeto: `printWidth: 100`, aspas simples.
