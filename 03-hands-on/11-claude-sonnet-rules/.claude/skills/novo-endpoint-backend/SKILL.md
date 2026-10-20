---
name: novo-endpoint-backend
description: Cria um endpoint REST no backend Spring Boot 4 deste projeto (controller → service → client → DTOs record → exceções → ApiExceptionHandler → testes). Use sempre que a tarefa pedir um novo GET/POST no backend, um novo código de erro ou uma nova validação de parâmetro.
---

# Novo endpoint no backend

Pré-requisitos: ler `.claude/rules/backend.md`, `arquitetura.md` e `diretorios.md`. Pacote base
`com.cezar.dev.airquality`, raiz `backend/src/main/java/com/cezar/dev/airquality/`.

## Passos

1. **Contrato** — anote do prompt da tarefa: caminho (sem `/api`), parâmetros e limites, campos da resposta,
   códigos de erro (`status` + `code` + mensagem PT-BR).
2. **DTO de resposta** em `dtos/` como `record`, campos em camelCase com os tipos do contrato
   (`Integer`/`Double` quando o campo puder ser `null`, primitivos quando nunca for).
3. **Exceções** em `exceptions/` (unchecked). Reaproveite as existentes; crie uma nova só quando houver um
   `code` novo. Cada exceção carrega o `code` e a mensagem:
   ```java
   public class InvalidRequestException extends RuntimeException {
       private final String code;
       public InvalidRequestException(String code, String message) { super(message); this.code = code; }
       public String getCode() { return code; }
   }
   ```
4. **`ErrorResponse`** — se ainda não existir: `public record ErrorResponse(String code, String message) {}`.
5. **`ApiExceptionHandler`** em `controllers/` (único `@RestControllerAdvice`). Um `@ExceptionHandler` por
   exceção própria, devolvendo `ResponseEntity<ErrorResponse>` com o status do contrato. Inclua também:
   - `MissingServletRequestParameterException` e `MethodArgumentTypeMismatchException` → 400 com o `code` de
     requisição inválida do endpoint (descubra qual pelo nome do parâmetro, se houver mais de um endpoint);
   - (opcional, só se o contrato da tarefa prever) um `Exception` genérico → 500 com mensagem genérica em
     PT-BR, sem detalhes.
6. **Client** (se o endpoint consome API externa): siga a skill `cliente-api-externa`.
7. **Service** em `services/` (`@Service`, injeção por construtor):
   - normalize (`trim`) e valide → lance a exceção própria;
   - chame o client, trate o caso "vazio" (ex. lista vazia → 404) e mapeie para o DTO de resposta;
   - traduções (código → texto, graus → direção etc.) como métodos puros e testáveis
     (`static` ou package-private), com valor padrão para entrada desconhecida.
8. **Controller** em `controllers/`: `@RestController`, `@GetMapping("/recurso")`, parâmetros com
   `@RequestParam(required = false)` quando a validação/mensagem deve ser a do projeto; só delega ao service.
9. **Properties**: URLs e timeouts novos em `application.properties`.
10. **Testes** (veja `.claude/rules/testes.md`):
    - `services/<Nome>ServiceTest`: validação, mapeamento, cada linha das tabelas de tradução;
    - `controllers/<Nome>ControllerTest`: cada status do contrato e o JSON exato do `ErrorResponse`.
    Esqueleto de teste de controller no Spring Boot 4:
    ```java
    @WebMvcTest(FooController.class)        // org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
    @Import(ApiExceptionHandler.class)
    class FooControllerTest {
        @Autowired MockMvc mvc;
        @MockitoBean FooService service;     // org.springframework.test.context.bean.override.mockito.MockitoBean

        @Test void retorna400QuandoNomeCurto() throws Exception {
            when(service.find("a")).thenThrow(new InvalidRequestException("INVALID_X", "..."));
            mvc.perform(get("/foo").param("name", "a"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.code").value("INVALID_X"));
        }
    }
    ```
    Se o import não resolver, confira o pacote atual com Context7 (Spring Boot 4 moveu os módulos de teste).
11. Rode `cd backend && ./mvnw test` e confirme `GET /healthcheck` intacto.

## Checklist

- [ ] Endpoint sem prefixo `/api`; sem CORS.
- [ ] Nenhuma regra de negócio no controller; nenhum HTTP fora de `clients/`.
- [ ] Todo erro sai como `{ "code", "message" }`, mensagem PT-BR, sem stack trace nem corpo externo.
- [ ] Nenhuma dependência nova no `pom.xml`.
