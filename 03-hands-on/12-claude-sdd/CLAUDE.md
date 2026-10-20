# CLAUDE.md

Monorepo com **backend** Spring Boot e **frontend** Angular com SSR, servidos na mesma origem pelo Traefik da VPS.
Funcionalidades que dependem de APIs externas passam sempre pelo backend.

## Stack

| Camada   | Tecnologia                                                                                     |
|----------|------------------------------------------------------------------------------------------------|
| Backend  | Java 25, Spring Boot 4.0.6 (`spring-boot-starter-webmvc`, `spring-boot-starter-restclient`), Maven Wrapper |
| Frontend | Angular 21 (standalone, signals, SSR com `@angular/ssr` + Express), TypeScript 5.9, Vitest, Prettier |
| Estilo   | CSS puro com variáveis em `src/styles.css`; Tailwind 4 instalado só com o `@import 'tailwindcss'` |
| Execução | `compose.yaml` (backend em Maven, frontend em `ng serve`) atrás do Traefik, sem `ports:`        |

## Mapa do repositório

```
compose.yaml                 # ambiente de dev (não alterar)
backend/                     # Spring Boot — pacote base com.cezar.dev.airquality
  src/main/java/com/cezar/dev/airquality/
    controllers/  services/  clients/  dtos/  exceptions/
  src/main/resources/application.properties
  src/test/java/com/cezar/dev/airquality/
frontend/                    # Angular 21 — projeto "air-quality"
  src/index.html  src/styles.css  src/server.ts
  src/app/                   # app.ts, app.html, app.config.ts, app.routes.ts
  src/app/<feature>/         # serviço de API + componentes da funcionalidade
```

Detalhes de onde cada arquivo fica: `.claude/rules/diretorios.md`.

## Comandos

```bash
# Backend (no host, processo que termina)
cd backend && ./mvnw test

# Frontend (no host, processo que termina)
cd frontend && npm test          # Vitest via @angular/build:unit-test
cd frontend && npm run build     # build com SSR
```

Subir o projeto, ver no browser ou depurar: siga `.claude/rules/execucao.md` e a skill global `sandbox`.

## Regras (`.claude/rules/`)

Carregadas automaticamente; as que têm `paths:` valem só para os arquivos correspondentes.

| Arquivo              | Domínio                                                                  |
|----------------------|--------------------------------------------------------------------------|
| `arquitetura.md`     | Fluxo Navegador → Backend → API externa, fronteiras entre camadas, URLs   |
| `diretorios.md`      | Onde cada tipo de arquivo vive e como é nomeado                          |
| `backend.md`         | Spring Boot: camadas, records, RestClient, exceções e corpo de erro      |
| `frontend.md`        | Angular 21: standalone, signals, `input()`/`output()`, HTTP, estado      |
| `ssr.md`             | O que nunca pode rodar no servidor                                       |
| `design-system.md`   | Tokens de cor, tipografia, espaçamento, raios, breakpoint, Tailwind      |
| `acessibilidade.md`  | Foco, contraste, ARIA, teclado, movimento reduzido                       |
| `testes.md`          | JUnit + `MockRestServiceServer`, Vitest + `HttpTestingController`        |
| `execucao.md`        | Compose, Traefik, prefixo de rota, `baseHref`                            |
| `restricoes.md`      | Arquivos intocáveis, dependências proibidas, o que nunca fazer           |

## Skills (`.claude/skills/`)

| Skill                    | Quando usar                                                              |
|--------------------------|--------------------------------------------------------------------------|
| `novo-endpoint-backend`  | Criar um endpoint REST (controller → service → client → DTOs → erros → testes) |
| `cliente-api-externa`    | Integrar uma API HTTP externa com `RestClient`, timeouts e tradução de erros |
| `novo-componente-angular`| Criar um componente standalone com `.ts`/`.html`/`.css` e tokens         |
| `servico-api-angular`    | Criar o serviço HTTP do frontend e o componente orquestrador de estado   |
| `validar-entrega`        | Rodar testes/build e checar as regras antes de declarar a tarefa pronta  |

## Prompts de tarefa

`prompt_1.md` descreve a funcionalidade atual (painel de clima). Ele só traz o que é específico da tarefa;
tudo que é convenção do projeto está nas regras e skills acima.
