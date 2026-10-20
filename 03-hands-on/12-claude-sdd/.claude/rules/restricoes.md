# Restrições do projeto

## Nunca

- O frontend chamar uma API externa diretamente.
- Fixar `/template/`, `/<VERSION>/` ou qualquer prefixo de rota no código.
- Expor stack trace, mensagem de exceção Java ou resposta bruta de API externa ao cliente.
- Quebrar `GET /healthcheck`.
- Usar cor, tamanho, espaçamento, sombra ou fonte fora dos tokens de `design-system.md`.

## Não alterar

- `compose.yaml` (nem adicionar `ports:`), Dockerfile (não criar), `angular.json`/`baseHref`.
- Configuração de CORS: não criar — frontend e backend estão na mesma origem via Traefik.

## Dependências

- Não adicionar dependências ao `pom.xml` nem ao `package.json`: nada de Angular Material, bibliotecas de
  ícones, axios, Lombok, WireMock, bibliotecas de data/estado etc.
- Único recurso externo permitido no frontend: o `<link>` da JetBrains Mono (Google Fonts).
- Tailwind permanece instalado só com o `@import 'tailwindcss'`; sem classes utilitárias nem `@theme`.

## Fora de escopo por padrão (só com pedido explícito na tarefa)

- Autenticação, banco de dados, cache, internacionalização, alternância de unidades, tema por
  `prefers-color-scheme`.
