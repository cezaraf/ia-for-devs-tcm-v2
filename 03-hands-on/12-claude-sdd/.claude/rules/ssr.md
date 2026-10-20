---
paths:
  - "frontend/src/**"
---

# SSR (Angular SSR habilitado)

- Nenhuma requisição ao backend durante a renderização no servidor.
- `navigator`, `window`, `document`, `localStorage`, `navigator.geolocation` e qualquer chamada HTTP só rodam
  no navegador: dentro de `afterNextRender(...)` ou protegidos por `isPlatformBrowser(inject(PLATFORM_ID))`.
- Disparo automático ao abrir a página (ex. buscar dados iniciais) vai em `afterNextRender` do componente
  orquestrador, nunca no construtor, em `ngOnInit` sem guarda ou em inicializador de campo.
- Decisões de template que dependem de API do navegador (ex. esconder um botão se
  `navigator.geolocation` não existir) usam um `signal` preenchido no navegador; no servidor assume-se o
  valor neutro (elemento oculto / estado inicial).
- Não alterar `src/server.ts`, `main.server.ts`, `app.config.server.ts` nem `app.routes.server.ts` sem pedido.
- `provideClientHydration(withEventReplay())` em `app.config.ts` permanece.
