# Testes

Uma tarefa só está pronta com `./mvnw test` (em `backend/`) e `npm test` (em `frontend/`) passando.
Ambos podem rodar no host (processos que terminam).

## Backend (JUnit 5, `spring-boot-starter-*-test` já presentes)

- Nunca chamar a API externa real: usar `MockRestServiceServer` ligado ao `RestClient.Builder` do client.
- Services: teste unitário cobrindo validação, mapeamento para DTO e toda tabela de tradução (cada entrada
  e o valor padrão para código desconhecido; limites de faixas como 0°, 22.5°, 360°).
- Controllers: `@WebMvcTest` (com o service/client mockado via `@MockitoBean`) ou teste de integração com
  `MockRestServiceServer`, cobrindo cada status do contrato (200/400/404/502/504) e o corpo `ErrorResponse`.
- Timeout simulado lançando `SocketTimeoutException` no `ResponseCreator`; falha de conexão com
  `ConnectException`/`IOException`; erro upstream com `withServerError()` / `withStatus(...)`.
- `AirqualityApplicationTests.contextLoads` continua passando.
- Sem WireMock nem outras dependências novas.

## Frontend (Vitest via `@angular/build:unit-test`)

- Serviço de API: `TestBed` com `provideHttpClient()` + `provideHttpClientTesting()`; verificar URL relativa,
  método, parâmetros e `httpMock.verify()` no `afterEach`.
- Componente orquestrador: mockar o serviço de API (objeto com `vi.fn()` retornando `of(...)` /
  `throwError(...)` / `Subject`) e cobrir cada transição de estado, retry e cancelamento de requisição antiga.
- Ao trocar o conteúdo de `app.html`, atualizar `app.spec.ts` para o novo conteúdo.
- Nenhum teste depende de rede, geolocalização real ou relógio real.
