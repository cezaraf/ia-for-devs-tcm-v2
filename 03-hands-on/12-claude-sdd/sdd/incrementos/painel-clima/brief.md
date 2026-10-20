# Brief: Painel de clima

## Identificação

- Incremento: painel-clima
- Origem: ideia
- Evidência da origem: não se aplica (rota `ideia`); solicitação em `prompt_1.md`
- Data: 2026-10-20
- Solicitação bruta: "Implemente um painel de clima no frontend e backend existente. O usuário deve poder
  digitar uma cidade e ver o clima atual." Dados via Open-Meteo (Geocoding + Forecast), sempre pelo
  backend; geolocalização do navegador opcional para sugerir a cidade.

## Intenção

Hoje a aplicação só tem o placeholder do Angular e o `GET /healthcheck`. O usuário quer abrir o painel,
informar o nome de uma cidade e ver o clima atual dela. O backend resolve a cidade em coordenadas e busca
o clima na Open-Meteo, entregando ao frontend um formato próprio e estável. Opcionalmente, o navegador
pode sugerir a cidade a partir da localização do usuário.

## Usuários e sistemas afetados

- Usuário final do painel (navegador, desktop e mobile).
- Frontend Angular 21 com SSR (`frontend/`): `app.html` deixa de ser placeholder.
- Backend Spring Boot 4 (`backend/`): novo endpoint, service, client(s), DTOs e tratamento de erro.
- APIs externas Open-Meteo: `geocoding-api.open-meteo.com/v1/search` e `api.open-meteo.com/v1/forecast`.
- Traefik/sandbox da VPS (apenas consumo; nada é alterado).

## Restrições inegociáveis

- Fluxo único Navegador → Backend → Open-Meteo; o frontend nunca chama API externa.
- Sem CORS, sem prefixo de rota fixo; URLs do frontend relativas (`api/...`), endpoints Spring sem `/api`.
- Sem dependências novas no `pom.xml`/`package.json`; `compose.yaml`, `angular.json` e `baseHref` intocados.
- Erros no corpo `ErrorResponse(code, message)`; nunca expor stack trace ou resposta bruta da Open-Meteo.
- Geolocalização só no navegador (nunca no SSR) e nunca persistida.
- Estilo só com tokens de `design-system.md`; requisitos de `acessibilidade.md`.
- `GET /healthcheck` continua funcionando; testes nunca acessam rede real.
- Fora de escopo: autenticação, banco, cache, i18n, alternância de unidades, tema por `prefers-color-scheme`.

## Classificação inicial

- Rigor: medium
- Risco: medio
- Autonomia: assistido
- Alvo do contrato: local
- Motivo: duas superfícies com contrato novo entre camadas, integração com duas APIs externas e UX
  relevante; sem segurança, billing, migração ou persistência. Localização do usuário é dado pessoal,
  mas fica só em memória e depende de consentimento do navegador, então o risco não é baixo nem alto.
  Alvo local porque o diretório não é repositório git (PRM-001).

## Superfícies e blast radius

- Superfícies: UI (Angular SSR), API (Spring), integração (Open-Meteo).
- Reversibilidade: alta. Funcionalidade nova e isolada, sem dados persistidos; reverter é remover os
  arquivos novos e restaurar `app.html`/`app.spec.ts`.
- Pior efeito plausível: painel quebrado ou travado em carregamento quando a Open-Meteo falhar ou demorar;
  vazamento de detalhes internos em mensagens de erro; regressão do `GET /healthcheck`; chamada de
  geolocalização no SSR derrubando a renderização.

## Contexto consultado

- Contratos vivos: nenhum (`sdd/contratos/` inexistente).
- Evidências: `prompt_1.md`; `CLAUDE.md`; `.claude/rules/*` (arquitetura, backend, frontend, ssr,
  design-system, acessibilidade, testes, execucao, restricoes, diretorios); `.claude/skills/*`;
  `backend/.../HealthCheckController.java`; `application.properties` (só `spring.application.name`);
  `frontend/src/app/app.html` (placeholder do Angular); `_comum.md`; `evals/cases/core-contracts.yaml`.
- Lacunas:
  - projeto não é repositório git: sem base SHA, PR ou merge (PRM-001);
  - harness SDD não instalado em `sdd/governanca` (PRM-006);
  - `CLAUDE.md` aponta `prompt_3.md` como funcionalidade atual, mas a entrada é `prompt_1.md` (PRM-007).

## Capacidades candidatas

- clima: NOVO — consulta do clima atual por nome de cidade (backend + painel).
- clima: NOVO — sugestão de cidade a partir da geolocalização do navegador (opcional).

## Perguntas para o PRD responder

- Quais campos do clima atual o painel exibe (temperatura, sensação térmica, umidade, vento e direção,
  condição/`weather_code` traduzida, dia/noite, horário da medição)?
- Quando a busca retorna várias cidades homônimas, o sistema escolhe a primeira, lista opções ou pede
  refinamento (estado/país)?
- Quais estados da UI são obrigatórios (inicial, carregando, sucesso, cidade não encontrada, erro
  upstream, timeout) e quais mensagens o usuário vê?
- Validações do texto da cidade: tamanho mínimo/máximo, caracteres aceitos, `trim`.
- A geolocalização entra neste incremento? Se sim, como uma coordenada vira "cidade sugerida" sem
  reverse geocoding na Open-Meteo, e qual o comportamento quando o usuário nega a permissão?
- Há retry manual após erro? A busca dispara por submit ou enquanto digita?
- Idioma dos nomes de cidade e das descrições de condição (PT-BR)?

## Decisões adiadas para TechSpec

- Formato do endpoint: um único recurso (`GET /weather?city=`) ou separação geocoding/clima; endpoint por
  coordenadas para a geolocalização.
- Um client por API externa (`GeocodingApiClient`, `ForecastApiClient`) e chaves de `application.properties`.
- Contrato JSON de resposta (`WeatherResponse`) e tabela de códigos de erro (`400`/`404`/`502`/`504`).
- Tabelas de tradução (WMO `weather_code` → texto, graus → direção do vento) e seus limites.
- Timeouts de conexão/leitura e mapeamento para `UPSTREAM_ERROR`/`UPSTREAM_TIMEOUT`.
- Máquina de estados do orquestrador Angular, cancelamento (`switchMap`) e guarda de SSR para geolocalização.
- Nomes de arquivos, classes e seletores dos componentes do painel.
