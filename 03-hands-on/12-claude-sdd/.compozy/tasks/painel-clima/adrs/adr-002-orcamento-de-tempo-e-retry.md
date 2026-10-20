# ADR-002 — Orçamento de tempo por chamada, teto de 15 s por operação e retry manual

- Status: aceito (auditoria do passo 04, 2026-10-20)
- Data: 2026-10-20
- Incremento: painel-clima
- Relacionados: RNF-002, RF-005, RF-006, RF-007, RF-009, BR-004, PRM-017

## Contexto

Nenhuma operação pode ficar em "Carregando" mais de 15 s (RNF-002). Uma operação pode envolver até duas
requisições ao backend (busca → clima) ou a espera da geolocalização seguida de uma requisição. O
`read-timeout` do `SimpleClientHttpRequestFactory` limita cada leitura do socket, não a duração total da
requisição. Não é permitido adicionar dependência de resiliência (Resilience4j etc.).

## Decisão

- Backend, por API externa: `connect-timeout=2s`, `read-timeout=4s` (pior caso prático ≈ 6 s por
  chamada); timeout → `504 UPSTREAM_TIMEOUT`; demais falhas → `502 UPSTREAM_ERROR`. Sem retry automático.
- Geolocalização: `getCurrentPosition` com `timeout: 8000`, `maximumAge: 300000`,
  `enableHighAccuracy: false`.
- Frontend: `timeout(15_000)` do RxJS sobre o observable **inteiro** de cada operação do usuário
  (`search`, `select`, `locate`), dentro do `switchMap`; estourar → estado `timeout`.
- Orçamento: busca com 1 cidade 6 + 6 = 12 s; escolha 6 s; localização 8 + 6 = 14 s **contados a
  partir da permissão concedida** — pela especificação W3C, o `timeout` do `getCurrentPosition` não
  conta o tempo do pedido de permissão, mas o teto de 15 s conta. O teto é absoluto em qualquer caso.
- Fase do estouro em `locate`: `TimeoutError` antes de a posição ser emitida → `geo-unavailable`
  (mensagem de localização, com retry — RF-009); depois → `timeout` ("Demora excessiva").
- "Tentar novamente" re-executa a **operação do usuário** que falhou (`lastFailed`), não a última
  chamada HTTP; disponível em `unavailable`, `timeout` e `geo-unavailable` (BR-004), exceto quando a
  falha é `400 INVALID_COORDINATES` (não transitória).

## Alternativas

1. **Retry automático com backoff (backend ou frontend)** — esconde falhas transitórias, mas consome o
   orçamento de 15 s e duplica carga num provedor já instável. Descartada.
2. **Só timeouts do backend, sem teto no frontend** — não cobre a soma de duas chamadas nem a espera da
   geolocalização; violaria RNF-002. Descartada.
3. **Timeout total por requisição no backend** (ex.: `JdkClientHttpRequestFactory` + `CompletableFuture`)
   — mais preciso, porém mais código e o teto do usuário continuaria precisando existir no frontend.
   Descartada por ora.

## Consequências

- O teto do usuário é garantido em um único ponto testável com fake timers.
- Respostas muito lentas porém contínuas (leituras abaixo de 4 s cada) podem passar de 6 s no backend;
  o teto do frontend cobre esse caso.
- Mudar PRM-017 altera só a constante do frontend e, se necessário, os valores em
  `application.properties`.
