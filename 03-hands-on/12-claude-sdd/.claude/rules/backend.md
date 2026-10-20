---
paths:
  - "backend/**"
---

# Backend — Spring Boot 4 / Java 25

- Pacote base `com.cezar.dev.airquality`; siga o padrão do `controllers/HealthCheckController` existente.
- Camadas e responsabilidades conforme `arquitetura.md`; estrutura conforme `diretorios.md`.
- Injeção por construtor (campos `final`); nada de `@Autowired` em campo. Sem Lombok.
- DTOs são sempre `record`s em `dtos/` — tanto as respostas da API quanto o JSON lido de APIs externas
  (records internos, com `@JsonProperty` quando o nome externo for snake_case).
- HTTP externo **somente** com `RestClient` (dependência `spring-boot-starter-restclient` já presente),
  dentro de `clients/`. Não usar `RestTemplate`, `WebClient`, OkHttp, Feign etc.
- URLs base e timeouts de APIs externas ficam em `application.properties` (chaves `<api>.<propriedade>`, ex.
  `foo-api.base-url`, `foo-api.connect-timeout=3s`), lidos por `@Value` ou `@ConfigurationProperties`.
  Nunca hardcoded no código.
- Validação de parâmetros no controller/service com **exceções próprias** de `exceptions/` (não usar
  Bean Validation para gerar o corpo de erro). Aplicar `trim` em textos antes de validar.
- Parâmetro obrigatório ausente ou não numérico também vira a exceção própria de requisição inválida
  (receba como `String`/opcional e converta, ou trate `MissingServletRequestParameterException` /
  `MethodArgumentTypeMismatchException` no `ApiExceptionHandler`).
- Todo erro sai pelo `controllers/ApiExceptionHandler` (`@RestControllerAdvice`) no corpo padrão
  `ErrorResponse` = `record ErrorResponse(String code, String message)`:
  - `code` em `UPPER_SNAKE_CASE`; `message` em PT-BR, legível pelo usuário;
  - nunca expor stack trace, nome de classe, mensagem de exceção do Java nem corpo bruto da API externa.
- Mapeamento padrão de falhas de API externa (feito no client, tratado no handler):
  - status ≠ 2xx, corpo inválido/incompleto ou falha de conexão → `502` `UPSTREAM_ERROR` (`UpstreamException`);
  - timeout de conexão ou de leitura → `504` `UPSTREAM_TIMEOUT` (`UpstreamTimeoutException`).
- Parâmetros inválidos → `400`; recurso inexistente → `404`, cada um com seu `code` específico.
- Respostas JSON em camelCase; números como número, nulos como `null` (não omitir campos).
- Sem banco de dados, cache, autenticação ou CORS (veja `restricoes.md`).
