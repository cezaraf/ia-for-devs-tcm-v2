# Diretórios e nomes de arquivo

## Backend — `backend/src/main/java/com/cezar/dev/airquality/`

| Subpacote      | Conteúdo                                                                                  | Exemplo                       |
|----------------|-------------------------------------------------------------------------------------------|-------------------------------|
| `controllers/` | `@RestController` (sufixo `Controller`) e o único `@RestControllerAdvice` (`ApiExceptionHandler`) | `HealthCheckController`        |
| `services/`    | `@Service` (sufixo `Service`)                                                              | `FooService`                  |
| `clients/`     | Um `@Component` por API externa (sufixo `Client`)                                          | `FooApiClient`                |
| `dtos/`        | `record`s de resposta (sufixo `Response`) e `record`s do JSON externo                     | `FooResponse`, `ErrorResponse` |
| `exceptions/`  | Exceções próprias (sufixo `Exception`), unchecked                                         | `InvalidRequestException`     |

- Configuração em `backend/src/main/resources/application.properties` (sem YAML).
- Testes em `backend/src/test/java/com/cezar/dev/airquality/` espelhando o subpacote da classe testada
  (`services/FooServiceTest`, `controllers/FooControllerTest`).
- Não criar subpacotes fora desta lista sem necessidade explícita.

## Frontend — `frontend/src/`

| Caminho                     | Conteúdo                                                              |
|-----------------------------|-----------------------------------------------------------------------|
| `index.html`                | `lang`, `<title>`, `<link>` da fonte                                   |
| `styles.css`                | `@import 'tailwindcss'` existente + **todos** os tokens CSS            |
| `app/app.ts`, `app.html`, `app.css` | Componente raiz `App` (`app-root`)                             |
| `app/app.config.ts`         | Providers do navegador (`provideHttpClient(withFetch())` etc.)         |
| `app/<feature>/`            | Serviço de API e componentes da funcionalidade                        |

- Padrão de nomes Angular 21 do projeto: `nome.ts` / `nome.html` / `nome.css` / `nome.spec.ts`, **sem**
  sufixo `.component` nem `.service`; classe sem sufixo `Component` (como `app.ts` → `App`).
  Ex.: `foo-card.ts` → classe `FooCard`, seletor `app-foo-card`; `foo-api.ts` → classe `FooApi`.
- Prefixo de seletor: `app-`.
- Testes ficam ao lado do arquivo testado (`foo-api.spec.ts`).
- Use exatamente os nomes de arquivo, classe e seletor definidos no prompt da tarefa.
