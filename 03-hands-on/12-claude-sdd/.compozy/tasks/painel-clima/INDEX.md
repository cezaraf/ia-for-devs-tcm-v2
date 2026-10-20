# INDEX — painel-clima

- PRD: `_prd.md` · TechSpec: `_techspec.md` · ADRs: `adrs/adr-001-recursos-cities-e-weather.md`,
  `adrs/adr-002-orcamento-de-tempo-e-retry.md`
- Plano: `sdd/incrementos/painel-clima/execucao.md` (revisão 1)
- Impacto contratual: `sdd/incrementos/painel-clima/impacto-contratual/clima/contrato.md` (domínio `clima`, NOVO)
- Base: `master` @ `d5bc7511b609b02754cc5d61e52d6356828fafcf`

## Tasks

| Task | Título | Tipo | Complexidade | Status | Dependências | Gherkin | Impacto |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `task_01` | Backend — fundação de erros e não regressão do healthcheck | backend | low | completed | — | `feature/001__task_01.feature` | clima: Formato único de erro do backend |
| `task_02` | Backend — clients Open-Meteo (Geocoding e Forecast) | backend | medium | completed | `task_01` | `feature/002__task_02.feature` | clima: Busca de cidades; Condições por coordenada; Formato de erro |
| `task_03` | Backend — endpoint GET /cities | backend | medium | completed | `task_02` | `feature/003__task_03.feature` | clima: Busca de cidades por nome |
| `task_04` | Backend — endpoint GET /weather e tabelas de tradução | backend | medium | completed | `task_02` | `feature/004__task_04.feature` | clima: Condições por coordenada; Tradução da condição; Direção do vento |
| `task_05` | Frontend — base da página, tokens e HttpClient | frontend | low | pending | — | `feature/005__task_05.feature` | clima: Acessibilidade; Tema dia/noite |
| `task_06` | Frontend — camada de dados | frontend | medium | completed | — | `feature/006__task_06.feature` | clima: Busca no painel; Falhas e retry; Localização; SSR |
| `task_07` | Frontend — componentes de apresentação do painel | frontend | medium | completed | `task_05`, `task_06` | `feature/007__task_07.feature` | clima: Busca; Homônimas; Exibição; Falhas; Acessibilidade |
| `task_08` | Frontend — orquestrador WeatherPanel e página inicial | frontend | high | completed | `task_06`, `task_07` | `feature/008__task_08.feature` | clima: Busca; Homônimas; Falhas; Mais recente; Localização; Tema; Acessibilidade; SSR |

## Cobertura por camada

| Camada | Tasks | SCN | TST |
| --- | --- | --- | --- |
| Backend — erros e não regressão | `task_01` | SCN-001, SCN-002 | TST-001, TST-002 |
| Backend — integração (clients) | `task_02` | SCN-003 a SCN-007 | TST-003, TST-004, TST-005 |
| Backend — API `/cities` | `task_03` | SCN-008 a SCN-013 | TST-006, TST-007 |
| Backend — API `/weather` | `task_04` | SCN-014 a SCN-019 | TST-008, TST-009 |
| Frontend — base e tokens | `task_05` | SCN-020 | TST-010 |
| Frontend — dados | `task_06` | SCN-021 a SCN-024 | TST-011 a TST-014 |
| Frontend — apresentação | `task_07` | SCN-025 a SCN-029 | TST-015 a TST-019 |
| Frontend — orquestração e SSR | `task_08` | SCN-030 a SCN-044 | TST-020, TST-021, TST-022 |
| Integração ponta a ponta | gate `validar-entrega` + QA (passo 08) | reexecuta SCN-001 a SCN-044 no ambiente `compose.yaml` | relatório de QA |

## Funcionalidades (FEAT)

| FEAT | Nome | Requisitos |
| --- | --- | --- |
| FEAT-001 | Busca de cidades por nome (backend) | RF-001, RF-002, RF-004; BR-001, BR-002, BR-003 |
| FEAT-002 | Condições atuais por coordenada (backend) | RF-003; BR-007, BR-011, BR-012 |
| FEAT-003 | Painel de busca e exibição do clima | RF-001 a RF-007; BR-001, BR-002, BR-004 a BR-007, BR-011, BR-012 |
| FEAT-004 | Clima pela localização do navegador | RF-008, RF-009; BR-008, BR-009, BR-010; RNF-003 |
| FEAT-005 | Formato de erro único e não regressão do backend | RNF-004, RNF-010; BR-006 |
| FEAT-006 | Identidade visual, tema e acessibilidade | RF-010; RNF-005 a RNF-008 |

## Matriz de rastreabilidade

`RF/RNF → BR → FEAT → Comportamento → SCN → task → TST`

| RF/RNF | BR | FEAT | Comportamento | SCN | Task | TST |
| --- | --- | --- | --- | --- | --- | --- |
| RNF-010 | — | FEAT-005 | (preservado) `GET /healthcheck` | SCN-001 | task_01 | TST-001 |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-002 | task_01 | TST-002 |
| RF-001 | BR-003 | FEAT-001 | Busca de cidades por nome | SCN-003 | task_02 | TST-003, TST-005 |
| RF-003 | BR-003, BR-011, BR-012 | FEAT-002 | Condições atuais por coordenada | SCN-004 | task_02 | TST-004, TST-005 |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-005 | task_02 | TST-003, TST-004 |
| RNF-002, RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-006 | task_02 | TST-003, TST-004 |
| RF-003, RNF-009 | BR-003 | FEAT-001, FEAT-002 | Busca de cidades por nome; Condições atuais por coordenada | SCN-007 | task_02 | TST-003, TST-004 |
| RF-001, RF-004 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-008 | task_03 | TST-006, TST-007 |
| RF-004 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-009 | task_03 | TST-006 |
| RF-005 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-010 | task_03 | TST-006, TST-007 |
| RF-002 | BR-001 | FEAT-001 | Busca de cidades por nome | SCN-011 | task_03 | TST-006, TST-007 |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-012 | task_03 | TST-006, TST-007 |
| RNF-002, RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-013 | task_03 | TST-007 |
| RF-003 | BR-011, BR-012 | FEAT-002 | Condições atuais por coordenada | SCN-014 | task_04 | TST-008, TST-009 |
| RF-003 | BR-007 | FEAT-002 | Condições atuais por coordenada | SCN-015 | task_04 | TST-008, TST-009 |
| RF-003 | BR-007 | FEAT-002 | Tradução da condição do tempo | SCN-016 | task_04 | TST-008 |
| RF-003 | BR-007 | FEAT-002 | Direção do vento por extenso | SCN-017 | task_04 | TST-008 |
| RF-008, RNF-004 | BR-006 | FEAT-005 | Condições atuais por coordenada; Formato único de erro do backend | SCN-018 | task_04 | TST-008, TST-009 |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-019 | task_04 | TST-008, TST-009 |
| RNF-005, RNF-008, RNF-011 | — | FEAT-006 | Acessibilidade do painel; Tema dia/noite | SCN-020 | task_05 | TST-010 |
| RNF-001 | BR-003 | FEAT-003 | Renderização no servidor sem efeitos do navegador (URLs relativas) | SCN-021 | task_06 | TST-011 |
| RF-002 | BR-001 | FEAT-003 | Busca por nome no painel | SCN-022 | task_06 | TST-012 |
| RF-005, RF-006 | BR-004, BR-006 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-023 | task_06 | TST-013 |
| RF-008, RNF-006 | BR-008 | FEAT-004 | Clima pela localização do navegador; Renderização no servidor sem efeitos do navegador | SCN-024 | task_06 | TST-014 |
| RF-001, RF-002, RF-008 | BR-005 | FEAT-003 | Busca por nome no painel; Acessibilidade do painel | SCN-025 | task_07 | TST-015 |
| RF-004 | BR-002 | FEAT-003 | Escolha entre cidades homônimas | SCN-026 | task_07 | TST-016 |
| RF-003 | BR-007, BR-011, BR-012 | FEAT-003 | Exibição das condições atuais | SCN-027 | task_07 | TST-017 |
| RF-005, RNF-005 | — | FEAT-006 | Carregamento, falhas e Tentar novamente; Acessibilidade do painel | SCN-028 | task_07 | TST-018 |
| RF-005, RF-006 | BR-004, BR-006 | FEAT-006 | Carregamento, falhas e Tentar novamente | SCN-029 | task_07 | TST-019 |
| RF-005, RF-010, RNF-006 | — | FEAT-006 | Renderização no servidor sem efeitos do navegador | SCN-030 | task_08 | TST-020, TST-022 |
| RF-002 | BR-001, BR-005 | FEAT-003 | Busca por nome no painel; Operação mais recente prevalece | SCN-031 | task_08 | TST-020 |
| RF-001, RF-003 | BR-002 | FEAT-003 | Busca por nome no painel | SCN-032 | task_08 | TST-020 |
| RF-004 | BR-002 | FEAT-003 | Escolha entre cidades homônimas | SCN-033 | task_08 | TST-020 |
| RF-005 | BR-002, BR-004 | FEAT-003 | Busca por nome no painel | SCN-034 | task_08 | TST-020 |
| RF-005, RF-006 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-035 | task_08 | TST-020 |
| RF-005, RF-006, RNF-002 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-036 | task_08 | TST-020 |
| RF-006 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-037 | task_08 | TST-020 |
| RF-007 | — | FEAT-003 | Operação mais recente prevalece | SCN-038 | task_08 | TST-020 |
| RF-008, RNF-003 | BR-008, BR-009, BR-010 | FEAT-004 | Clima pela localização do navegador | SCN-039 | task_08 | TST-020 |
| RF-009 | BR-004 | FEAT-004 | Clima pela localização do navegador | SCN-040 | task_08 | TST-020 |
| RF-009, RNF-002 | BR-004 | FEAT-004 | Clima pela localização do navegador | SCN-041 | task_08 | TST-020 |
| RF-010 | — | FEAT-006 | Tema dia/noite | SCN-042 | task_08 | TST-020 |
| RNF-005 | — | FEAT-006 | Acessibilidade do painel | SCN-043 | task_08 | TST-020 |
| RF-001, RNF-006 | — | FEAT-003 | Busca por nome no painel (página inicial) | SCN-044 | task_08 | TST-021, TST-022 |

Cobertura reversa: RF-001 a RF-010, BR-001 a BR-012 e os RNF-001 a RNF-011, **exceto RNF-007**, aparecem ao menos
uma vez na matriz. RNF-007 (320 px, empilhamento abaixo de 640 px) é implementado na task_07
(`@media (max-width: 639px)`) e verificado só no QA manual: não há SCN automatizado para ele (sem framework E2E;
ver risco R-14 em `execucao.md`). RNF-009 e RNF-011 são garantidos pelas regras de teste de cada task e pela
varredura `validar-entrega`.

## Premissas que afetam o plano

PRM-012 a PRM-020 confirmadas e PRM-021 rejeitada pelo dev em 2026-10-20 (PRM-022). Os defaults já refletidos
nas tasks valem como decisão; qualquer mudança futura exige revisão do plano e nova auditoria.
