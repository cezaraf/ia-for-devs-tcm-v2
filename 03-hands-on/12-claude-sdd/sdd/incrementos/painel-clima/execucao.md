# Plano de execução — painel-clima

## Identidade da base

- Base ref: `master`
- Base SHA: `d5bc7511b609b02754cc5d61e52d6356828fafcf` (commit inicial "chore: commit inicial do projeto e abertura
  do incremento painel-clima")
- Gerado em: 2026-10-20
- Revisão do plano: 1
- Observação: `_prd.md`, `_techspec.md`, `adrs/`, este plano, as tasks, os `.feature` e o impacto contratual ainda
  **não estão versionados** (fora do Base SHA). Ver risco R-01.

## Rota efetiva

- TechSpec: obrigatória — rigor `medium`, duas superfícies (UI + API) com contrato novo entre camadas.
- Review: obrigatório — rigor `medium`, risco `medio` e entrega com PR/merge.
- PR/merge: obrigatório — `alvo_contrato: branch` (PRM-001); passos 10 e 11 com gate humano (autonomia `assistido`).
- Deploy/verificação: dispensado — alvo `branch`, sem produção; a execução manual usa o `compose.yaml` do projeto em
  `https://test.cezar.dev/<VERSION>/` (PRM-008), apenas para QA.
- Passo 05 (harness em `sdd/governanca`): não incluído na rota; recomendado antes do 04 (PRM-006). Sem ele,
  `sdd-guard.sh`/`sdd-fluxo.sh` no caminho canônico ficam indisponíveis e os gates são conferidos manualmente.
- Etapas: 00 → 01 → 02 → 03 → 04 → 06 → 07 → 08 → 10 → 11 → 13 → 14.

## Objetivo operacional

Entregar o domínio `clima` completo nas duas superfícies: backend Spring com `GET /cities` e `GET /weather`
(clients Open-Meteo, validação, traduções e `ErrorResponse`) e frontend Angular SSR com o painel na página inicial
(busca por nome, escolha entre homônimas, clima atual, localização opcional, estados, retry, tema e acessibilidade),
com cada `SCN-*` provado por teste automatizado verde, `GET /healthcheck` preservado e nenhum arquivo protegido
alterado. Uma entrega só de backend ou só de frontend é incompleta.

## Ordem de implementação

| Ordem | Task | Dependências | Resultado independente |
| --- | --- | --- | --- |
| 1 | `task_01` — Backend: fundação de erros e healthcheck | — | `ErrorResponse`, exceções e `ApiExceptionHandler` testados; healthcheck coberto por teste |
| 2 | `task_02` — Backend: clients Open-Meteo | `task_01` | Clients com URLs exatas e tradução de falhas, testados com `MockRestServiceServer` |
| 3 | `task_03` — Backend: `GET /cities` | `task_02` | Endpoint de cidades completo (200/400/404/502/504) |
| 3 | `task_04` — Backend: `GET /weather` e traduções | `task_02` | Endpoint de clima completo com tabelas WMO e vento |
| 1 | `task_05` — Frontend: base, tokens e HttpClient | — | Página pt-BR, fonte, tokens dos dois temas, `provideHttpClient(withFetch())` |
| 1 | `task_06` — Frontend: camada de dados | — (contrato da TechSpec) | `WeatherApi`, `BrowserLocation`, validação e mensagens testados |
| 2 | `task_07` — Frontend: componentes de apresentação | `task_05`, `task_06` | Cinco componentes standalone testados isoladamente |
| 3 | `task_08` — Frontend: orquestrador e página inicial | `task_06`, `task_07` | Painel funcional com estados, retry, cancelamento, tema e SSR |

Trilhas backend (`01 → 02 → 03 ∥ 04`) e frontend (`05 ∥ 06 → 07 → 08`) são paralelizáveis: o frontend implementa contra
o contrato fixado na TechSpec ("Contratos entre camadas") e mocka `WeatherApi`. A integração real entre as camadas
é provada no gate de validação (skill `validar-entrega`) e no QA (passo 08), depois das oito tasks.

## Arquivos e superfícies esperadas

| Task | Arquivo/diretório | Tipo de mudança | Motivo |
| --- | --- | --- | --- |
| task_01 | `backend/.../dtos/ErrorResponse.java` | novo | Corpo de erro único (RNF-004) |
| task_01 | `backend/.../exceptions/{InvalidRequest,CityNotFound,Upstream,UpstreamTimeout}Exception.java` | novo | Exceções próprias unchecked |
| task_01 | `backend/.../controllers/ApiExceptionHandler.java` | novo | Tradução exceção → status/`code`/mensagem |
| task_01 | `backend/src/test/.../controllers/{ApiExceptionHandler,HealthCheckController}Test.java` | novo | TST-002, TST-001 |
| task_02 | `backend/src/main/resources/application.properties` | alterado | Chaves `geocoding-api.*` e `forecast-api.*` |
| task_02 | `backend/.../clients/{OpenMeteoConfig,GeocodingApiClient,ForecastApiClient}.java` | novo | Única camada de HTTP de saída |
| task_02 | `backend/.../dtos/{GeocodingApiResponse,ForecastApiResponse}.java` | novo | JSON externo tolerante |
| task_02 | `backend/src/test/.../clients/{GeocodingApiClient,ForecastApiClient}Test.java` | novo | TST-003, TST-004 |
| task_03 | `backend/.../dtos/CityResponse.java`, `services/CityService.java`, `controllers/CityController.java` | novo | `GET /cities` |
| task_03 | `backend/src/test/.../{services/CityServiceTest,controllers/CityControllerTest}.java` | novo | TST-006, TST-007 |
| task_04 | `backend/.../dtos/WeatherResponse.java`, `services/WeatherService.java`, `controllers/WeatherController.java` | novo | `GET /weather` e traduções |
| task_04 | `backend/src/test/.../{services/WeatherServiceTest,controllers/WeatherControllerTest}.java` | novo | TST-008, TST-009 |
| task_05 | `frontend/src/index.html` | alterado | `lang`, título, fonte |
| task_05 | `frontend/src/styles.css` | alterado | Tokens dos dois temas e base |
| task_05 | `frontend/src/app/app.config.ts` | alterado | `provideHttpClient(withFetch())` |
| task_06 | `frontend/src/app/weather/{weather-api,browser-location,city-name,weather-messages}.ts` + `.spec.ts` | novo | Camada de dados (TST-011 a TST-014) |
| task_07 | `frontend/src/app/weather/{city-search-form,city-options,weather-card,weather-skeleton,status-message}.{ts,html,css,spec.ts}` | novo | Apresentação (TST-015 a TST-019) |
| task_08 | `frontend/src/app/weather/weather-panel.{ts,html,css,spec.ts}` | novo | Orquestrador (TST-020) |
| task_08 | `frontend/src/app/app.{ts,html,css,spec.ts}` | alterado | Página inicial = painel (TST-021) |

Arquivos que **não** podem aparecer no diff: `compose.yaml`, `frontend/angular.json` (exceto a reescrita do `baseHref`
pelo compose no bind mount, PRM-009), `backend/pom.xml`, `frontend/package.json`/`package-lock.json`, `Dockerfile`,
`frontend/src/server.ts`, `main.server.ts`, `app.config.server.ts`, `app.routes.server.ts`, `HealthCheckController.java`,
`sdd/contratos/`.

## Contratos e dados

| Contrato/dado | Mudança | Compatibilidade | Rollback |
| --- | --- | --- | --- |
| `GET /cities?name=` → `CityResponse[]` | NOVO | Interno (PRM-003); evolução só aditiva | Reverter o merge do incremento |
| `GET /weather?latitude=&longitude=` → `WeatherResponse` | NOVO | Interno; evolução só aditiva | Reverter o merge do incremento |
| `ErrorResponse(code, message)` e tabela de códigos | NOVO | Códigos estáveis; mensagens fixas da TechSpec | Reverter o merge do incremento |
| `application.properties` (`geocoding-api.*`, `forecast-api.*`) | NOVO | Chaves novas, nenhuma existente alterada | Reverter o merge do incremento |
| `GET /healthcheck` | Preservado | Inalterado (teste novo TST-001) | Não se aplica |
| Página inicial (placeholder → painel) | Substituído | Placeholder não é contrato | Reverter `app.*`, `index.html`, `styles.css`, `app.config.ts` |
| Localização do usuário | Uso só em memória, arredondada a 2 casas | Sem persistência nem log (BR-009) | Não se aplica |

Sem banco, cache, migração ou armazenamento no navegador.

## Pontos de risco

| ID | Risco | Probabilidade | Impacto | Mitigação | Sinal de parada |
| --- | --- | --- | --- | --- | --- |
| R-01 | PRD, TechSpec, ADRs e artefatos do passo 03 não versionados; `base_sha` não os contém (resolvido: commit da especificação autorizado pelo dev em 2026-10-20 e registrado como Evidence SHA da auditoria) | alta | médio | Pedir ao dev commit dos artefatos de especificação antes do passo 06 (autonomia `assistido`: o agente não commita) | Implementação iniciada sem os artefatos versionados |
| R-02 | Premissas PRM-012 a PRM-019 (resolvido: confirmadas pelo dev em 2026-10-20, PRM-022) (campos, limites 2–100, lista de homônimas, geolocalização por clique, "Sua localização", teto 15 s, idioma, tema após erro) | média | alto | Defaults já refletidos nas tasks; reversões mapeadas na TechSpec ("Premissas e perguntas abertas herdadas") | Dev reverter qualquer premissa → revisão do plano e reauditoria antes do passo 06 |
| R-03 | PRM-019 × RF-010 (resolvido: dev escolheu PRM-019 e o RF-010 foi realinhado em 2026-10-20) | média | baixo | Plano segue PRM-019 (SCN-042); divergência explícita na TechSpec | Dev preferir RF-010 literal → ajustar SCN-042 e task_08 |
| R-04 | PRM-020 (`subregion` no contrato) (resolvido: aceita pelo dev em 2026-10-20) | baixa | médio | Campo nullable e sufixo só em colisão (SCN-008, SCN-026) | Dev rejeitar → remover o campo das tasks 03, 06 e 07 antes do passo 06 |
| R-05 | PRM-021 (auto-seleção de capital) **rejeitada pelo dev em 2026-10-20**; sem ela a lista aparece para a maioria dos nomes reais | alta | médio | BR-002 literal + filtro PPL*; nenhuma task implementa PRM-021 | Dev adotar PRM-021 → nova revisão do plano, novos SCN e reauditoria |
| R-06 | (resolvido no passo 04: PRD e TechSpec reescritos sem os termos) `reject_placeholders` do guard casa, sem distinção de caixa, a palavra portuguesa que coincide com o marcador de pendência (t-o-d-o): `_prd.md` (BR-007, RNF-005, RNF-009) e `_techspec.md` (linhas 14 e 747) seriam reprovados quando o harness for instalado | alta | médio | Reportado ao dev; artefatos do passo 03 escritos sem a palavra | Guard reprovando PRD/TechSpec no passo 04/06 |
| R-07 | SCN-020 (task_05) e parte de SCN-030/SCN-044 (TST-022) são provados por comando automatizado (build + `grep`), não por arquivo de teste | média | baixo | Comandos fixados nas tasks, saída registrada com o rótulo do SCN | Auditoria exigir teste em arquivo → revisar a prova |
| R-14 | RNF-007 (responsividade 320 px a desktop) sem SCN automatizado; provado só no QA manual | média | médio | `@media (max-width: 639px)` revisado no review (passo 07); verificação a 320 px no QA (passo 08) | QA encontrar rolagem horizontal ou formulário sem empilhar |
| R-08 | Teste de backend acessar a rede real | baixa | alto | `@MockitoBean` do service nos controllers; clients só com `MockRestServiceServer`; sem `@SpringBootTest` com override de `RestClient` | Teste lento ou instável sem rede |
| R-09 | Divergência de validação do nome entre navegador e backend (NBSP, U+FEFF, emoji) | média | médio | Mesmo conjunto `White_Space` + U+FEFF e contagem por code points; mesma matriz em TST-006 e TST-012 | `400 INVALID_CITY_NAME` vindo do backend para texto aceito pelo navegador |
| R-10 | Acesso a `navigator`/HTTP no prerender quebrando o build SSR | média | alto | `isPlatformBrowser`, chamadas só em handlers, `npm run build` em TST-010/TST-022 | Build falha ou erro de hidratação |
| R-11 | Horário exibido no fuso do navegador | baixa | médio | `observedAt.slice(11, 16)`; proibido `new Date(observedAt)` (SCN-027) | QA vê hora diferente da hora local da cidade |
| R-12 | Contrato TS divergir do JSON do backend (trilhas paralelas) | média | alto | Tipos e fixtures do TST-011 iguais aos exemplos JSON da TechSpec; testes de controller fixam nomes e nulabilidade | Erro de campo no QA integrado |
| R-13 | Harness SDD não instalado (PRM-006): gates determinísticos indisponíveis | alta | médio | Rodar o passo 05 antes do 04; até lá, conferência manual dos gates | Auditoria sem o guard disponível |

## Alternativas descartadas

- Task única full-stack: viola a separação backend/frontend exigida quando a TechSpec indica duas superfícies e
  impede trilhas paralelas.
- Task de "validação integrada" separada: a validação (skill `validar-entrega`, `compose.yaml` e QA) é gate e passo 08,
  não task de implementação; manter como gate evita task sem diff.
- Frontend dependente do backend no grafo: serializaria as trilhas sem ganho, já que o frontend implementa contra o
  contrato da TechSpec e mocka `WeatherApi`.
- Um `.feature` por comportamento: dificultaria ligar cenário a task; adotado um `.feature` por task, com tags
  `@FEAT-*` por cenário quando a task cruza funcionalidades.
- Endpoint único, retry automático, handler genérico, `@SpringBootTest` com `@Primary RestClient`, 16 pontos de
  direção, `Date`/`DatePipe`: descartados na TechSpec e nos ADR-001/ADR-002.

## Provas de conclusão

| TST/SCN | Evidência | Comando/fluxo | Resultado esperado |
| --- | --- | --- | --- |
| TST-001 / SCN-001 | `HealthCheckControllerTest` | `cd backend && ./mvnw test` | `200 "OK"` |
| TST-002 / SCN-002 | `ApiExceptionHandlerTest` | `cd backend && ./mvnw test` | Status, `code` e mensagem exatos; corpo só com 2 campos |
| TST-003, TST-004 / SCN-003 a SCN-007 | `GeocodingApiClientTest`, `ForecastApiClientTest` | `cd backend && ./mvnw test` | URLs exatas; falhas → `Upstream*Exception` |
| TST-005 / SCN-003, SCN-004 | `AirqualityApplicationTests.contextLoads` | `cd backend && ./mvnw test` | Contexto sobe com os dois `RestClient` e as properties |
| TST-006, TST-007 / SCN-008 a SCN-013 | `CityServiceTest`, `CityControllerTest` | `cd backend && ./mvnw test` | 200/400/404/502/504 com corpo exato |
| TST-008, TST-009 / SCN-014 a SCN-019 | `WeatherServiceTest`, `WeatherControllerTest` | `cd backend && ./mvnw test` | Tabelas completas; 200/400/502/504 |
| TST-010 / SCN-020 | Build + conferência do HTML e de `styles.css` | `cd frontend && npm run build` + `grep` da task_05 | `lang="pt-BR"`, título, fonte, tokens day/night |
| TST-011 a TST-014 / SCN-021 a SCN-024 | Specs da camada de dados | `cd frontend && npm test` | Verdes, sem rede nem geolocalização real |
| TST-015 a TST-019 / SCN-025 a SCN-029 | Specs dos componentes | `cd frontend && npm test` | Verdes |
| TST-020 / SCN-030 a SCN-043 | `weather-panel.spec.ts` | `cd frontend && npm test` | Cada transição; fake timers 15 s |
| TST-021 / SCN-044 | `app.spec.ts` | `cd frontend && npm test` | `app-weather-panel` presente |
| TST-022 / SCN-030, SCN-044 | Prerender | `cd frontend && npm run build` | HTML inicial sem clima e sem botão de localização |
| Gate de regras | Varredura `validar-entrega` | Comandos da skill, seção 2 e 3 | Sem prefixo fixo, host externo, CORS, cor/sombra literal, dependência nova, compose alterado |
| QA manual (passo 08) | Relatório de QA | `compose.yaml` em `https://test.cezar.dev/<VERSION>/` | `api/healthcheck` → `OK`; "Pindamonhangaba" direto; "Recife" e "São José" em lista; "xyzqwkk" não encontrada; "a" sem requisição; localização concedida/negada; dia/noite; 320 px sem rolagem; teclado, leitor de tela, contraste e `prefers-reduced-motion` |

## Checklist de tasks

- [ ] `task_01` — Backend: fundação de erros e não regressão do healthcheck
- [ ] `task_02` — Backend: clients Open-Meteo (Geocoding e Forecast)
- [ ] `task_03` — Backend: endpoint GET /cities
- [ ] `task_04` — Backend: endpoint GET /weather e tabelas de tradução
- [ ] `task_05` — Frontend: base da página, tokens e HttpClient
- [ ] `task_06` — Frontend: camada de dados (WeatherApi, BrowserLocation, validação e mensagens)
- [ ] `task_07` — Frontend: componentes de apresentação do painel
- [ ] `task_08` — Frontend: orquestrador WeatherPanel e página inicial

## Gates

- [ ] Auditoria de especificação e plano (passo 04)
- [ ] Gate humano, quando exigido — especificação: dispensado (risco `medio`, autonomia `assistido`, sem contrato
      público, dados, autorização ou migração); PR e merge: requeridos
- [x] `compozy tasks validate --name painel-clima`
- [ ] lint/typecheck/build/test/e2e — `./mvnw test`, `npm test`, `npm run build`, varredura `validar-entrega`; E2E não
      se aplica (sem framework E2E; proibido adicionar)
- [ ] review (passo 07)
- [ ] QA (passo 08)
- [ ] PR/merge, quando aplicável (passos 10 e 11, com gate humano)
- [ ] deploy/verificação, quando aplicável — dispensado (alvo `branch`)
- [ ] fechamento

## Aprovação do plano

### Parecer do agente

- Status: PENDENTE
- Evidência: `compozy tasks validate --name painel-clima` → "all tasks valid (8 scanned)"; matriz completa em
  `.compozy/tasks/painel-clima/INDEX.md`; aguarda auditoria do passo 04.

### Gate humano

- Requerido: não
- Status: dispensado
- Aprovado por:
- Escopo: risco `medio`, autonomia `assistido`, sem contrato público, autorização, billing, dados ou migração
  (conforme `gates.especificacao` do `incremento.yaml`).
- Data:

## Revisões do plano

| Revisão | Data | Motivo | Impacto | Reauditoria |
| --- | --- | --- | --- | --- |
| 1 | 2026-10-20 | Plano inicial (passo 03) | — | Não se aplica |
