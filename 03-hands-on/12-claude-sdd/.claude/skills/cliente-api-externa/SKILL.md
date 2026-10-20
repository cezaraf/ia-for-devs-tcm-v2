---
name: cliente-api-externa
description: Integra uma API HTTP externa no backend com RestClient (Spring Boot 4), URLs e timeouts em application.properties, tradução de falhas para 502 UPSTREAM_ERROR / 504 UPSTREAM_TIMEOUT e testes com MockRestServiceServer. Use ao criar ou alterar qualquer classe em clients/.
---

# Cliente de API externa (`clients/`)

Regras aplicáveis: `.claude/rules/backend.md`, `testes.md`, `restricoes.md` (sem dependências novas).

## 1. Configuração em `application.properties`

```properties
foo-api.base-url=https://api.exemplo.com/v1
foo-api.connect-timeout=3s
foo-api.read-timeout=5s
```

Use os nomes de chave e valores definidos no prompt da tarefa.

## 2. Bean do `RestClient` (timeouts) e classe do client

O `RestClient` com timeouts é montado num `@Configuration` de produção; o client recebe o `RestClient`
pronto. Assim o teste injeta um `RestClient` ligado ao `MockRestServiceServer` sem que nada sobrescreva a
request factory do mock.

```java
@Configuration
public class FooApiConfig {

    @Bean
    RestClient fooApiRestClient(RestClient.Builder builder,
                                @Value("${foo-api.connect-timeout}") Duration connectTimeout,
                                @Value("${foo-api.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return builder.requestFactory(factory).build();
    }
}
```

(A classe de configuração pode ficar em `clients/`, ao lado do client.)

```java
@Component
public class FooApiClient {

    private final RestClient restClient;
    private final String baseUrl;

    public FooApiClient(RestClient fooApiRestClient, @Value("${foo-api.base-url}") String baseUrl) {
        this.restClient = fooApiRestClient;
        this.baseUrl = baseUrl;
    }

    public FooApiResponse fetch(String name) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/search")
                .queryParam("name", name).encode().build().toUri();
        try {
            FooApiResponse body = restClient.get().uri(uri).retrieve().body(FooApiResponse.class);
            if (body == null) throw new UpstreamException();
            return body;
        } catch (RestClientResponseException e) {          // status != 2xx
            throw new UpstreamException();
        } catch (ResourceAccessException e) {              // I/O: timeout ou conexão
            if (isTimeout(e)) throw new UpstreamTimeoutException();
            throw new UpstreamException();
        } catch (RestClientException e) {                  // corpo inválido / desserialização
            throw new UpstreamException();
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) return true;
        }
        return false;
    }
}
```

Notas:
- URIs completas por chamada (`UriComponentsBuilder` + `queryParam`, encoding correto), o que permite um só
  `RestClient` para APIs com hosts diferentes (ex. busca e previsão, cada uma com sua URL em properties).
  Nunca concatenar strings.
- Se houver mais de um bean `RestClient` no contexto, use `@Qualifier` ou nomes de parâmetro distintos.
- JSON externo em `record`s internos de `dtos/` com `@JsonProperty("snake_case")`. Campos obrigatórios
  ausentes → `UpstreamException` (valide no client ou no service).
- Nunca registrar nem repassar o corpo bruto da resposta externa ao cliente.

## 3. Testes com `MockRestServiceServer`

Regra de ouro: depois de `bindTo(builder)`, **nada** pode chamar `requestFactory(...)` no builder — senão o
mock é substituído e o teste vai à internet.

```java
class FooApiClientTest {
    private MockRestServiceServer server;
    private FooApiClient client;

    @BeforeEach void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FooApiClient(builder.build(), "https://api.exemplo.com/v1");
    }

    @Test void sucesso() {
        server.expect(requestTo(startsWith("https://api.exemplo.com/v1/search")))
              .andExpect(queryParam("name", "Curitiba"))
              .andRespond(withSuccess("{...}", MediaType.APPLICATION_JSON));
        // asserts...
        server.verify();
    }

    @Test void erro5xxVira502() {
        server.expect(anything()).andRespond(withServerError());
        assertThrows(UpstreamException.class, () -> client.fetch("x"));
    }

    @Test void timeoutVira504() {
        server.expect(anything()).andRespond(request -> { throw new SocketTimeoutException("Read timed out"); });
        assertThrows(UpstreamTimeoutException.class, () -> client.fetch("x"));
    }

    @Test void conexaoRecusadaVira502() {
        server.expect(anything()).andRespond(request -> { throw new ConnectException("refused"); });
        assertThrows(UpstreamException.class, () -> client.fetch("x"));
    }
}
```

Testes de controller que atravessam o client real (status 502/504 de ponta a ponta) substituem o bean
`RestClient` por um ligado ao mock:

```java
@SpringBootTest
@AutoConfigureMockMvc   // Boot 4: org.springframework.boot.webmvc.test.autoconfigure
class FooControllerIT {
    @TestConfiguration
    static class MockConfig {
        static final RestClient.Builder BUILDER = RestClient.builder();
        static final MockRestServiceServer SERVER = MockRestServiceServer.bindTo(BUILDER).build();
        @Bean @Primary RestClient fooApiRestClient() { return BUILDER.build(); }
    }
    // @BeforeEach: MockConfig.SERVER.reset();
}
```

Os timeouts reais não são exercitados no teste: o comportamento é simulado lançando a exceção no
`ResponseCreator`.

## Checklist

- [ ] Única classe que fala com aquela API; ninguém mais usa `RestClient` para ela.
- [ ] URLs/timeouts só em `application.properties`.
- [ ] Não-2xx, corpo inválido/incompleto, conexão → `UpstreamException` (502); timeout → `UpstreamTimeoutException` (504).
- [ ] Testes nunca acessam a internet (nenhum `requestFactory(...)` depois do `bindTo`).
