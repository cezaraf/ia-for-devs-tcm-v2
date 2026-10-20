# Auditoria de especificação e plano — painel-clima

## Resumo

- Status: PRONTO
- Data: 2026-10-20
- Evidence SHA: c18b424423820403408827ca57073e5f8781b876
- Rigor: medium
- Risco: medio
- Autonomia: assistido
- Alvo do contrato: branch

## Parecer do agente

- Status: PRONTO
- Agente: Claude Opus 5.5 (Claude Code), auditor do passo 04 em contexto isolado do autor dos artefatos
- Evidência: leitura integral de `_comum.md`, `incremento.yaml`, `brief.md`, `execucao.md`, `impacto-contratual/` (README e
  `clima/contrato.md`), `_prd.md`, `_techspec.md`, ADR-001, ADR-002, `INDEX.md`, `task_01.md` a `task_08.md`, os oito
  `.feature`, `prompt_1.md`, `CLAUDE.md` e `.claude/rules/*`; correções documentais e decisões do dev (PRM-022) versionadas
  nos commits `85c52f5` e `c18b424`; `compozy tasks validate --name painel-clima` → `all tasks valid (8 scanned)`; varredura com a
  mesma regex de `reject_placeholders` do guard, com `/usr/bin/grep` em `LC_ALL=C`, sobre PRD, TechSpec, plano, INDEX,
  tasks e este relatório, sem ocorrência; `governanca/sdd-guard.sh pre-specify painel-clima` passa pelas checagens
  de placeholders, tasks, grafo, status, rastreabilidade e formato/existência/ancestralidade do Evidence SHA e para em "authority.check_command vazio;
  nenhuma aprovacao e presumida" / "autoridade externa nao comprovada para 'audit_evidence'" (AUD-010), antes da comparação do snapshot; essa comparação foi feita à mão:
  `git diff --stat c18b424` sobre os dois diretórios do incremento lista só `incremento.yaml`, com mudanças apenas em
  `status`, `fase`, `gates.especificacao.parecer_agente` e `metricas.data_especificado` (campos excluídos pela
  normalização do guard), e o único arquivo não versionado é este relatório.
- Limitações:
  - harness SDD não instalado em `sdd/governanca/` (PRM-006): não há `policies.yaml` nem checker externo de autoridade
    (`authority.check_command`); por construção, `audit_evidence` falha fechado no guard e os gates determinísticos ficam
    em conferência manual; por decisão do dev em 2026-10-20: o harness de governança não será usado neste projeto (passo 05 dispensado, AUD-010);
  - não existe contrato vivo (`sdd/contratos/` ausente, PRM-002): a consistência foi verificada só entre impacto
    contratual, PRD, TechSpec, ADRs e plano;
  - nenhuma API externa foi chamada nesta auditoria; o comportamento da Open-Meteo descrito na TechSpec (sondagem de
    2026-10-20) foi aceito como evidência do passo 02 e será reconfirmado no QA.

## Gate humano

- Requerido: não
- Motivo: risco `medio`, autonomia `assistido`, sem contrato público (PRM-003), autorização, billing, dados persistidos,
  migração nem remoção de comportamento do contrato vivo (o placeholder do framework não é contrato, AUD-007); nenhum
  fator da matriz mínima de `_comum.md` se aplica. As decisões de produto pendentes foram tomadas pelo dev nesta
  auditoria e registradas em PRM-022, mas não constituem gate obrigatório. PR e merge mantêm gate humano requerido e
  pendente.
- Status: dispensado
- Aprovado por: não se aplica (dispensado)
- Escopo: especificação e plano do incremento painel-clima no commit `c18b424`; não autoriza commit de implementação,
  push, PR, merge nem deploy.
- Data: 2026-10-20

## Achados

| ID | Severidade | Artefato | Evidência | Correção | Status |
| --- | --- | --- | --- | --- | --- |
| AUD-001 | P1 | Todos os artefatos do passo 01 ao 03 | PRD, TechSpec, ADRs, plano, tasks, `.feature` e impacto contratual não versionados; `d5bc751` não os continha (risco R-01) | Dev autorizou um commit local sem push (PRM-022): `85c52f5`; `c18b424` (correção do AUD-013) foi feito pelo auditor na leitura de que o escopo autorizado cobre os artefatos de especificação, e foi ratificado pelo dev em 2026-10-20; `c18b424` é o Evidence SHA | Fechado |
| AUD-002 | P1 | `_prd.md` (PRM-012 a PRM-019, Perguntas abertas 1 a 4) | Premissas de produto marcadas como pendentes de confirmação; o auditor não pode decidir produto | Dev confirmou o pacote em 2026-10-20 (PRM-022); PRD, TechSpec, INDEX e plano atualizados | Fechado |
| AUD-003 | P2 | `_prd.md` RF-010 × PRM-019; `_techspec.md` (estado do orquestrador); `impacto-contratual/clima/contrato.md` | RF-010 dizia "estados sem cidade → dia"; PRM-019, TechSpec e impacto contratual mantinham o tema após erro e na lista | Dev escolheu PRM-019; RF-010 realinhado; nota de divergência da TechSpec removida; SCN-042 inalterado | Fechado |
| AUD-004 | P2 | `_techspec.md` PRM-020 e PRM-021; `execucao.md` R-04 e R-05 | `subregion` no contrato `/cities` e auto-seleção de capital pendentes do dev | Dev aceitou PRM-020 e rejeitou PRM-021 (PRM-022); textos e riscos atualizados | Fechado |
| AUD-005 | P2 | `_prd.md` linhas 174, 175, 199 e 210; `_techspec.md` linhas 14 e 747 | A regex de `reject_placeholders` casa, sem distinção de caixa, a palavra que coincide com o marcador de pendência e a expressão de lacuna citada do prompt (risco R-06) | Trechos reescritos ("o círculo inteiro", "qualquer", "cada", "seção de lacunas reservadas à TechSpec"); nova varredura sem ocorrência | Fechado |
| AUD-006 | P3 | `task_03.md` linhas 47 e 48; `task_06.md` linhas 54 e 55; `feature/006__task_06.feature` linha 27 | Casos de teste com U+00A0, U+FEFF e U+2007 literais: dois casos visualmente idênticos (`" a "`) e um invisível | Substituídos pela notação escapada já usada na TechSpec e em `feature/003__task_03.feature` | Fechado |
| AUD-007 | P2 | `_prd.md` (Impacto no contrato vivo) | Placeholder do framework rotulado como `REMOVIDO`, contra o impacto contratual ("não entra como REMOVIDO"); remoção de comportamento exigiria gate humano | Reescrito como "Substituído (sem impacto `REMOVIDO`)" | Fechado |
| AUD-008 | P3 | `INDEX.md`; `execucao.md` R-14 | RNF-007 (320 px, empilhamento abaixo de 640 px) sem SCN automatizado; não há framework E2E e adicioná-lo é proibido | Aceito como risco registrado: `@media (max-width: 639px)` conferido no review (passo 07) e 320 px no QA (passo 08) | Aceito (registrado) |
| AUD-009 | P3 | `task_05.md` (TST-010), `task_08.md` (TST-022); `execucao.md` R-07 | SCN-020 e parte de SCN-030/SCN-044 provados por comando automatizado (build + `grep`) e não por arquivo de teste | Aceito: comandos fixados nas tasks, iniciados por `echo` com os IDs, saída registrada como evidência; o review confere | Aceito (registrado) |
| AUD-010 | P2 | Governança (`sdd/governanca/` ausente, PRM-006, R-13) | Sem `policies.yaml`, hooks nem checker externo: `sdd-guard.sh pre-specify painel-clima` não consegue comprovar `audit_evidence`, e as regras críticas (escrita em `sdd/contratos/`, implementação sem auditoria) dependem de disciplina | Por decisão do dev em 2026-10-20: o harness de governança não será usado neste projeto; passo 05 não será executado e os gates (auditoria `PRONTO` antes do passo 06, escrita em `sdd/contratos/` só no passo 13, P0/P1 antes de merge, autorização de commit/PR/merge) são conferidos manualmente pelo agente e pelo dev em cada passo | Fechado (dispensado pelo dev) |
| AUD-011 | P3 | `incremento.yaml` (`responsavel`) | Campo `responsavel` vazio | Decisão do dev; informar o nome altera `incremento.yaml` (material) e exige novo commit e reauditoria | Aberto |
| AUD-013 | P2 | `_techspec.md` linhas 155, 449, 461 e 682; `task_06.md` linha 52; `governanca/sdd-guard.sh` (`reject_placeholders`) | Em `LC_ALL=C`, letra acentuada é fronteira de palavra: o equivalente português de "method" (com e acentuado) casa com o marcador de pendência e o guard reprovava a TechSpec (`sdd-guard.sh pre-specify` → "placeholder nao resolvido") | Redação trocada para "verbo"/"função" (commit `c18b424`); defeito do guard levado ao passo 14 como aprendizado, sem alterar governança | Fechado nos artefatos; aprendizado aberto |
| AUD-012 | P3 | `task_08.md` | Task `high` com 15 cenários (SCN-030 a SCN-044) | Aceito: um único orquestrador coeso, e dividir quebraria a tabela de transições; subtasks e testes por transição explícitos | Aceito (registrado) |

Nenhum P0/P1 aberto.

## Rastreabilidade

| RF/RNF | BR | FEAT | Comportamento | SCN | Task | TST | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RNF-010 | — | FEAT-005 | (preservado) `GET /healthcheck` | SCN-001 | task_01 | TST-001 | Planejado; TST citado na task |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-002 | task_01 | TST-002 | Planejado; TST citado na task |
| RF-001 | BR-003 | FEAT-001 | Busca de cidades por nome | SCN-003 | task_02 | TST-003, TST-005 | Planejado; TST citado na task |
| RF-003 | BR-003, BR-011, BR-012 | FEAT-002 | Condições atuais por coordenada | SCN-004 | task_02 | TST-004, TST-005 | Planejado; TST citado na task |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-005 | task_02 | TST-003, TST-004 | Planejado; TST citado na task |
| RNF-002, RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-006 | task_02 | TST-003, TST-004 | Planejado; TST citado na task |
| RF-003, RNF-009 | BR-003 | FEAT-001, FEAT-002 | Busca de cidades por nome; Condições atuais por coordenada | SCN-007 | task_02 | TST-003, TST-004 | Planejado; TST citado na task |
| RF-001, RF-004 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-008 | task_03 | TST-006, TST-007 | Planejado; TST citado na task |
| RF-004 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-009 | task_03 | TST-006 | Planejado; TST citado na task |
| RF-005 | BR-002 | FEAT-001 | Busca de cidades por nome | SCN-010 | task_03 | TST-006, TST-007 | Planejado; TST citado na task |
| RF-002 | BR-001 | FEAT-001 | Busca de cidades por nome | SCN-011 | task_03 | TST-006, TST-007 | Planejado; TST citado na task |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-012 | task_03 | TST-006, TST-007 | Planejado; TST citado na task |
| RNF-002, RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-013 | task_03 | TST-007 | Planejado; TST citado na task |
| RF-003 | BR-011, BR-012 | FEAT-002 | Condições atuais por coordenada | SCN-014 | task_04 | TST-008, TST-009 | Planejado; TST citado na task |
| RF-003 | BR-007 | FEAT-002 | Condições atuais por coordenada | SCN-015 | task_04 | TST-008, TST-009 | Planejado; TST citado na task |
| RF-003 | BR-007 | FEAT-002 | Tradução da condição do tempo | SCN-016 | task_04 | TST-008 | Planejado; TST citado na task |
| RF-003 | BR-007 | FEAT-002 | Direção do vento por extenso | SCN-017 | task_04 | TST-008 | Planejado; TST citado na task |
| RF-008, RNF-004 | BR-006 | FEAT-005 | Condições atuais por coordenada; Formato único de erro do backend | SCN-018 | task_04 | TST-008, TST-009 | Planejado; TST citado na task |
| RNF-004 | BR-006 | FEAT-005 | Formato único de erro do backend | SCN-019 | task_04 | TST-008, TST-009 | Planejado; TST citado na task |
| RNF-005, RNF-008, RNF-011 | — | FEAT-006 | Acessibilidade do painel; Tema dia/noite | SCN-020 | task_05 | TST-010 | Planejado; TST citado na task |
| RNF-001 | BR-003 | FEAT-003 | Renderização no servidor sem efeitos do navegador (URLs relativas) | SCN-021 | task_06 | TST-011 | Planejado; TST citado na task |
| RF-002 | BR-001 | FEAT-003 | Busca por nome no painel | SCN-022 | task_06 | TST-012 | Planejado; TST citado na task |
| RF-005, RF-006 | BR-004, BR-006 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-023 | task_06 | TST-013 | Planejado; TST citado na task |
| RF-008, RNF-006 | BR-008 | FEAT-004 | Clima pela localização do navegador; Renderização no servidor sem efeitos do navegador | SCN-024 | task_06 | TST-014 | Planejado; TST citado na task |
| RF-001, RF-002, RF-008 | BR-005 | FEAT-003 | Busca por nome no painel; Acessibilidade do painel | SCN-025 | task_07 | TST-015 | Planejado; TST citado na task |
| RF-004 | BR-002 | FEAT-003 | Escolha entre cidades homônimas | SCN-026 | task_07 | TST-016 | Planejado; TST citado na task |
| RF-003 | BR-007, BR-011, BR-012 | FEAT-003 | Exibição das condições atuais | SCN-027 | task_07 | TST-017 | Planejado; TST citado na task |
| RF-005, RNF-005 | — | FEAT-006 | Carregamento, falhas e Tentar novamente; Acessibilidade do painel | SCN-028 | task_07 | TST-018 | Planejado; TST citado na task |
| RF-005, RF-006 | BR-004, BR-006 | FEAT-006 | Carregamento, falhas e Tentar novamente | SCN-029 | task_07 | TST-019 | Planejado; TST citado na task |
| RF-005, RF-010, RNF-006 | — | FEAT-006 | Renderização no servidor sem efeitos do navegador | SCN-030 | task_08 | TST-020, TST-022 | Planejado; TST citado na task |
| RF-002 | BR-001, BR-005 | FEAT-003 | Busca por nome no painel; Operação mais recente prevalece | SCN-031 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-001, RF-003 | BR-002 | FEAT-003 | Busca por nome no painel | SCN-032 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-004 | BR-002 | FEAT-003 | Escolha entre cidades homônimas | SCN-033 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-005 | BR-002, BR-004 | FEAT-003 | Busca por nome no painel | SCN-034 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-005, RF-006 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-035 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-005, RF-006, RNF-002 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-036 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-006 | BR-004 | FEAT-003 | Carregamento, falhas e Tentar novamente | SCN-037 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-007 | — | FEAT-003 | Operação mais recente prevalece | SCN-038 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-008, RNF-003 | BR-008, BR-009, BR-010 | FEAT-004 | Clima pela localização do navegador | SCN-039 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-009 | BR-004 | FEAT-004 | Clima pela localização do navegador | SCN-040 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-009, RNF-002 | BR-004 | FEAT-004 | Clima pela localização do navegador | SCN-041 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-010 | — | FEAT-006 | Tema dia/noite | SCN-042 | task_08 | TST-020 | Planejado; TST citado na task |
| RNF-005 | — | FEAT-006 | Acessibilidade do painel | SCN-043 | task_08 | TST-020 | Planejado; TST citado na task |
| RF-001, RNF-006 | — | FEAT-003 | Busca por nome no painel (página inicial) | SCN-044 | task_08 | TST-021, TST-022 | Planejado; TST citado na task |

Cobertura reversa conferida: RF-001 a RF-010, BR-001 a BR-012 e RNF-001 a RNF-011 aparecem na matriz, exceto RNF-007
(AUD-008). Cada SCN de SCN-001 a SCN-044 está em exatamente um `.feature` e é citado pela sua task; cada TST
(TST-001, TST-002, TST-003, TST-004, TST-005, TST-006, TST-007, TST-008, TST-009, TST-010, TST-011, TST-012, TST-013,
TST-014, TST-015, TST-016, TST-017, TST-018, TST-019, TST-020, TST-021, TST-022) está em uma task, com comando de prova em
`execucao.md`. Cada comportamento do impacto contratual tem ao menos um SCN.

## Plano

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Base identificada | OK | `execucao.md`: `master` @ `d5bc751`; especificação auditada em `c18b424` (sobre `85c52f5`), descendente de `d5bc751` |
| Rota derivada de rigor, risco e alvo | OK | TechSpec obrigatória (`medium`), review obrigatório (`medium`/`medio`/PR), PR/merge obrigatórios (alvo `branch`), deploy dispensado com justificativa (sem produção) |
| Passo 05 fora da rota | OK | Dispensa registrada em `rota.justificativas_de_dispensa` e confirmada por decisão do dev em 2026-10-20: o harness de governança não será usado neste projeto (AUD-010) |
| Arquivos esperados por task | OK | Tabela "Arquivos e superfícies esperadas" e seção "Limites" de cada task coincidem |
| Arquivos proibidos no diff | OK | `compose.yaml`, `angular.json`, `pom.xml`, `package.json`, `server.ts` e SSR, `HealthCheckController`, `sdd/contratos/` listados no plano |
| Ordem e grafo de dependências | OK, acíclico | `01 → 02 → (03 ∥ 04)`; `05 ∥ 06 → 07 → 08`; frontmatter das tasks coincide com o plano |
| Riscos com mitigação e sinal de parada | OK | R-01 a R-14; R-01 a R-06 resolvidos nesta auditoria |
| Alternativas descartadas | OK | Plano, TechSpec e ADR-001/ADR-002 |
| Provas de conclusão | OK | Comando por TST; `./mvnw test`, `npm test`, `npm run build`, varredura `validar-entrega`, QA via `compose.yaml` (PRM-008) |
| Testes sem rede, geolocalização ou relógio reais | OK | `MockRestServiceServer`, `@MockitoBean` do service, `HttpTestingController`, stub de `navigator.geolocation`, `vi.useFakeTimers()`; `@SpringBootTest` com `@Primary RestClient` proibido com justificativa |
| Arquitetura (Navegador → Backend → Open-Meteo) | OK | URLs `api/...` relativas; endpoints sem `/api`; clients únicos; sem CORS; sem prefixo fixo |
| Segurança e privacidade | OK | `ErrorResponse` fixo sem `reason` do provedor; URIs com encoding; SSRF fechado; localização só por clique, arredondada a 2 casas, sem log nem persistência; risco residual de log do Traefik registrado |
| Acessibilidade | OK | `lang`, `label`, `aria-live`/`aria-busy`, `role="alert"`, foco movido, `prefers-reduced-motion`; contraste pelos pares de tokens; RNF-007 no QA |
| Desempenho e operação | OK | Timeouts 2 s/4 s, teto de 15 s com fases de `locate` (ADR-002); logs `WARN` sem dados sensíveis; runbook de dev |
| Gherkin observável | OK | Cenários com entradas e saídas concretas (status, `code`, textos, atributos ARIA, rótulos) |
| Gates humanos | OK | Especificação dispensada com justificativa; PR e merge requeridos e pendentes; produção dispensada |
| Ações não autorizadas | Nenhuma | `85c52f5` autorizado pelo dev (PRM-022); `c18b424`, só com a redação de `_techspec.md` e `task_06.md`, feito na leitura de escopo do auditor e ratificado pelo dev em 2026-10-20 (pergunta 4); relatório e transição do `incremento.yaml` commitados com autorização do dev; sem push, PR, merge ou deploy; nenhum arquivo protegido ou de código alterado |

## Cobertura por superfície

| Superfície | Tasks | Contratos | Testes | Status |
| --- | --- | --- | --- | --- |
| Backend — erros e healthcheck | task_01 | `ErrorResponse`; `GET /healthcheck` preservado | TST-001, TST-002 | Coberto |
| Integração Open-Meteo | task_02 | Geocoding `/search`, Forecast `/forecast`; properties `geocoding-api.*`, `forecast-api.*` | TST-003, TST-004, TST-005 | Coberto |
| API `GET /cities` | task_03 | `CityResponse[]`, 400/404/502/504 | TST-006, TST-007 | Coberto |
| API `GET /weather` | task_04 | `WeatherResponse`, tabelas WMO e vento, 400/502/504 | TST-008, TST-009 | Coberto |
| Frontend — base, tokens, HttpClient | task_05 | Tokens de `design-system.md`; `provideHttpClient(withFetch())` | TST-010 | Coberto (prova por comando, AUD-009) |
| Frontend — dados | task_06 | Tipos TS espelhando a TechSpec; `api/cities`, `api/weather` | TST-011, TST-012, TST-013, TST-014 | Coberto |
| Frontend — apresentação | task_07 | `input()`/`output()` da TechSpec | TST-015, TST-016, TST-017, TST-018, TST-019 | Coberto |
| Frontend — orquestração e SSR | task_08 | Estados, retry, cancelamento, tema, prerender | TST-020, TST-021, TST-022 | Coberto |
| Dados e migração | — | Sem banco, cache ou armazenamento no navegador | — | Não se aplica |
| Responsividade (RNF-007) | task_07 | `@media (max-width: 639px)` | Review e QA manual | Aceito sem SCN (AUD-008) |

## Validação Compozy

- Comando: `compozy tasks validate --name painel-clima`
- Resultado: `all tasks valid (8 scanned)` (antes e depois das correções, e no estado do commit `c18b424`)

## Perguntas/decisões humanas

1. Respondidas em 2026-10-20 (PRM-022): PRM-012 a PRM-018 confirmadas; PRM-019 prevalece sobre a redação antiga de
   RF-010; PRM-020 aceita; PRM-021 rejeitada; commit local da especificação autorizado, sem push.
2. Respondida em 2026-10-20: o harness de governança (passo 05, `sdd-guard.sh`, hooks, policy e checker de autoridade)
   não será usado; os gates seguem em conferência manual (AUD-010).
3. Aberta, não bloqueante: quem é o `responsavel` do incremento (AUD-011)?
4. Respondida em 2026-10-20: o dev ratificou o commit `c18b424` (AUD-013) e autorizou o commit deste relatório e da
   transição do `incremento.yaml`.

## Conclusão

A especificação e o plano do painel-clima são coerentes entre impacto contratual, PRD, TechSpec, ADRs, plano, tasks e
cenários. Cada requisito tem cenário observável e teste planejado, exceto RNF-007, aceito como risco de QA. O grafo de
tasks é acíclico e separa backend e frontend. Arquitetura, segurança, privacidade, acessibilidade e operação seguem as
regras do projeto. As decisões de produto que bloqueavam a auditoria foram tomadas pelo dev e versionadas nos commits
`85c52f5` e `c18b424`; este último é a evidência deste parecer. Não há P0/P1 aberto e o gate humano de especificação não é exigido, então o
incremento pode passar para `especificado`. Por decisão do dev, o harness de governança não é usado: o
enforcement determinístico (guard, hooks, checker de autoridade) não existe, e cada passo a partir do 06 confere os gates
manualmente. Qualquer mudança material posterior no snapshot auditado exige novo commit e reauditoria.
