# Documento de Requisitos do Produto

## Relação com o incremento

- Brief: `sdd/incrementos/painel-clima/brief.md`
- Contratos vivos consultados: nenhum — `sdd/contratos/` não existe (PRM-002). Também consultados como
  contexto: `prompt_1.md`, `CLAUDE.md`, `.claude/rules/*` (arquitetura, frontend, ssr, acessibilidade,
  design-system, backend, restricoes, execucao) e `incremento.yaml`.
- Reclassificação: **alvo do contrato `local` → `branch`**. Motivo: em 2026-10-20 o dev autorizou `git init`
  e o commit inicial e definiu `alvo_contrato: branch` (PRM-001); a rota passou a incluir os passos 10 (PR) e
  11 (merge). O brief, congelado, mantém a classificação inicial. Rigor (`medium`), risco (`medio`) e
  autonomia (`assistido`) permanecem: nada no PRD acrescenta autenticação, persistência, billing, migração ou
  contrato público (PRM-003).

## Problema e valor

Quem abre a aplicação hoje vê apenas a página padrão do framework: não há nenhuma funcionalidade útil. O
usuário quer saber rapidamente como está o tempo agora em uma cidade, sem sair da página, sem criar conta
e sem precisar conhecer coordenadas. O painel de clima entrega essa consulta em poucos segundos, com
mensagens claras quando algo dá errado, e é a primeira capacidade de negócio do produto, estabelecendo o
padrão de integração com serviços externos que as próximas funcionalidades vão seguir.

### Comportamento atual

- A página inicial exibe o conteúdo de exemplo do framework (placeholder), sem busca nem dados de clima.
- O backend responde apenas a `GET /healthcheck`, que deve continuar funcionando sem alteração.

### Comportamento alvo

- A página inicial passa a ser o painel de clima: o usuário informa uma cidade e vê as condições atuais
  dela; opcionalmente, pede para usar a própria localização.
- Todos os dados de clima e de localização de cidades chegam ao navegador **somente pelo backend** da
  aplicação, que consulta o provedor Open-Meteo.

## Resultado pretendido

- O usuário obtém o clima atual de uma cidade digitando o nome e confirmando a busca, sem nenhum outro
  passo quando o nome é inequívoco.
- Quando o nome é ambíguo, o usuário escolhe a cidade certa em uma lista, sem precisar reescrever a busca.
- Em qualquer falha (cidade inexistente, serviço fora do ar, demora excessiva), o usuário vê uma mensagem
  compreensível em PT-BR e sabe o que fazer a seguir; o painel nunca fica preso em "carregando".
- O painel é utilizável apenas por teclado e por leitor de tela.

## Métricas de sucesso

Não há analytics no produto (e adicioná-lo está fora de escopo); as métricas usam as fontes que o fluxo SDD
já produz.

- Cobertura de requisitos: 100% dos RF/BR com ao menos um cenário `SCN-*` com teste automatizado verde —
  fonte: tasks e relatório de testes (passos 03/06); janela: até o fim do passo 07.
- Estados da UI verificados: os 8 estados de RF-005 observados no ambiente de desenvolvimento
  (`https://test.cezar.dev/<VERSION>/`) — fonte: relatório de QA (passo 08); janela: execução do QA.
- Defeitos: zero `P0`/`P1` abertos ao fim do QA — fonte: relatório de QA; janela: passo 08 até o merge.
- Tempo de resposta percebido: nenhuma busca permanece em "carregando" além do teto de RNF-002 — fonte:
  testes automatizados com falha simulada de demora + QA; janela: passos 06 a 08.
- Acessibilidade: zero violações do checklist de `acessibilidade.md` (foco, contraste, rótulos, região
  dinâmica, alertas, teclado) — fonte: review (07) e QA (08).
- Não regressão: `GET /healthcheck` responde com sucesso antes e depois do incremento — fonte: testes e QA.

## Personas e jornadas

- Como **visitante do painel**, quero digitar o nome de uma cidade e confirmar, para ver a temperatura e as
  condições do tempo agora naquele lugar.
- Como **visitante que digitou um nome comum a várias cidades** (ex.: "São José"), quero escolher em uma
  lista qual delas eu queria, para não ver o clima do lugar errado.
- Como **visitante no celular**, quero pedir ao painel que use minha localização, para ver o clima de onde
  estou sem digitar nada.
- Como **visitante que negou o acesso à localização**, quero continuar usando a busca por nome normalmente,
  para não ficar bloqueado.
- Como **visitante que usa teclado ou leitor de tela**, quero ser avisado do carregamento, do resultado e
  dos erros e ter o foco levado ao lugar onde preciso agir.
- Como **visitante quando o serviço de clima falha ou demora**, quero uma mensagem clara e um jeito de
  tentar novamente, para não recarregar a página.

## Requisitos funcionais

### RF-001 — Buscar clima atual por nome de cidade

O painel exibe um campo de texto rotulado para o nome da cidade e um botão de busca. Ao confirmar a busca
(botão ou tecla Enter), o painel exibe o clima atual da cidade correspondente (RF-003) ou o estado
adequado de RF-005. A busca é disparada somente por confirmação, nunca enquanto o usuário digita (BR-005).

### RF-002 — Validar o texto informado antes de buscar

Antes de qualquer consulta, o texto tem espaços removidos do início e do fim. Texto vazio, só com espaços,
ou fora dos limites de BR-001 não dispara busca: o painel mostra uma mensagem de validação em PT-BR e
devolve o foco ao campo. O backend aplica a mesma regra e rejeita entradas inválidas que chegarem a ele com
uma mensagem equivalente, sem consultar o provedor externo.

### RF-003 — Exibir as condições atuais da cidade

Em caso de sucesso, o painel mostra, para a cidade resolvida:

- identificação da cidade: nome, região/estado (quando existir) e país;
- temperatura do ar (°C);
- sensação térmica (°C);
- umidade relativa do ar (%);
- velocidade do vento (km/h) e direção do vento por extenso em PT-BR (ex.: "Nordeste"), derivada do ângulo;
- condição do tempo em texto PT-BR (ex.: "Céu limpo", "Chuva moderada"), com texto padrão para condição
  desconhecida;
- indicação de dia ou noite no local, com marcador textual `[ DIA ]` / `[ NOITE ]`;
- horário da medição, no horário local da cidade.

Campos que o provedor não informar aparecem como indisponíveis ("—"), sem quebrar o painel (BR-007).

### RF-004 — Escolher entre cidades homônimas

Quando o nome corresponde a mais de uma cidade, o painel exibe uma lista de opções, cada uma identificada por
nome, região/estado e país, e move o foco para o primeiro item. Ao escolher uma opção (clique ou Enter), o
painel exibe o clima daquela cidade. Quando há exatamente uma correspondência, o clima é exibido direto,
sem lista. Regra de quantidade em BR-002.

### RF-005 — Estados da interface

O painel tem os estados abaixo, mutuamente exclusivos na área de resultado:

| Estado                     | O que o usuário vê                                                                 | Tentar novamente |
|----------------------------|------------------------------------------------------------------------------------|------------------|
| Inicial                    | Campo vazio e instrução curta do que fazer; nenhum resultado                       | —                |
| Carregando                 | Indicação de carregamento no formato do resultado final (sem salto de layout)      | —                |
| Sucesso                    | Condições atuais (RF-003)                                                          | —                |
| Escolha de cidade          | Lista de cidades homônimas (RF-004)                                                | —                |
| Entrada inválida           | Mensagem de validação; foco no campo                                               | Não (corrigir)   |
| Cidade não encontrada      | Mensagem informando que nenhuma cidade com aquele nome foi encontrada              | Não (corrigir)   |
| Serviço indisponível       | Mensagem informando que o serviço de clima está indisponível no momento            | Sim              |
| Demora excessiva           | Mensagem informando que o serviço de clima demorou demais para responder           | Sim              |

Os textos exatos das mensagens são definidos na TechSpec; devem seguir BR-006.

### RF-006 — Tentar novamente

Nos estados "Serviço indisponível" e "Demora excessiva", o painel oferece a ação "Tentar novamente", que
repete exatamente a última operação que falhou (mesma busca por nome, mesma cidade escolhida na lista ou
mesma consulta por localização), sem o usuário redigitar nada.

### RF-007 — Nova busca substitui a anterior

Se o usuário iniciar uma nova busca (ou escolher outra cidade, ou pedir a localização) enquanto uma anterior
ainda está em andamento, o painel passa a refletir apenas a operação mais recente; o resultado de uma
operação antiga, mesmo que chegue depois, nunca aparece nem altera o estado.

### RF-008 — Usar a localização do navegador (opcional)

Quando o navegador oferece geolocalização, o painel mostra a ação "Usar minha localização". Ao acioná-la, o
navegador pede permissão; concedida, o painel exibe o clima atual do local do usuário com os mesmos campos de
RF-003 (a identificação do local segue PRM-016). Regras em BR-008 a BR-010.

### RF-009 — Geolocalização negada ou indisponível

Se o navegador não suportar geolocalização, a ação de RF-008 não aparece. Se o usuário negar a permissão,
se a posição não puder ser obtida ou se a obtenção demorar demais, o painel mostra uma mensagem informativa
em PT-BR e permanece totalmente utilizável pela busca por nome (RF-001); nenhum outro estado é bloqueado.

### RF-010 — Tema dia/noite

A aparência do painel (cores) segue o tema "dia" ou "noite" do local exibido: dia quando é dia no local do
resultado, noite quando é noite. Antes do primeiro resultado vale o tema "dia"; em erros e na lista de
escolha, o tema permanece o do último resultado exibido (PRM-019).
A troca de tema acontece sem animação. O tema nunca segue a preferência de cor do sistema operacional.

## Regras de negócio

- BR-001: o nome da cidade, após remover espaços nas extremidades, deve ter entre **2 e 100 caracteres**
  (PRM-013); fora disso a entrada é inválida e nenhuma consulta externa é feita.
- BR-002: a busca considera no máximo **10** cidades candidatas (PRM-014); com 0 → "Cidade não encontrada";
  com 1 → clima direto; com 2 a 10 → lista de escolha, na ordem de relevância devolvida pelo provedor.
- BR-003: dados de clima e de cidades chegam ao navegador exclusivamente pelo backend da aplicação; o
  navegador nunca contata o provedor externo.
- BR-004: "Tentar novamente" só é oferecido para falhas repetíveis (serviço indisponível, demora excessiva,
  falha transitória da localização); entrada inválida e cidade não encontrada exigem correção do usuário.
- BR-005: a busca por nome é disparada apenas por confirmação explícita (botão ou Enter); não há busca
  automática enquanto o usuário digita.
- BR-006: mensagens exibidas ao usuário são em PT-BR, curtas, orientadas à ação e nunca contêm detalhes
  internos (nomes técnicos, trechos de exceção, rastros de pilha ou resposta bruta do provedor).
- BR-007: a direção do vento é expressa por extenso em PT-BR a partir do ângulo, cobrindo o círculo inteiro
  (0° a 360°, com 360° equivalente a 0°); a condição do tempo é traduzida para texto PT-BR e qualquer código de
  condição desconhecido recebe um texto padrão (ex.: "Condição desconhecida"). A granularidade da direção
  (8 ou 16 pontos) e as tabelas completas são definidas na TechSpec.
- BR-008: a localização do usuário só é solicitada após ação explícita do usuário (RF-008), nunca ao abrir
  a página nem durante a renderização no servidor (PRM-004, PRM-015).
- BR-009: a localização do usuário é usada apenas para a consulta em curso; não é armazenada (nem no
  navegador nem no servidor) e não aparece em logs (PRM-004).
- BR-010: a coordenada usada na consulta por localização é a informada pelo navegador; o painel não exibe a
  coordenada com precisão maior que a necessária para identificar o local (detalhe na TechSpec).
- BR-011: unidades fixas — temperatura em °C, vento em km/h, umidade em %; não há alternância de unidades.
- BR-012: o horário da medição é exibido no fuso horário local da cidade consultada.

## Requisitos não funcionais

- RNF-001 (arquitetura/segurança): o navegador só se comunica com o backend da própria aplicação, na mesma
  origem; nenhuma configuração de compartilhamento entre origens é criada; o painel funciona sob qualquer
  prefixo de publicação, sem prefixo fixo no código.
- RNF-002 (desempenho/resiliência): nenhuma operação permanece no estado "Carregando" por mais de
  **15 segundos** (PRM-017); ao atingir o teto, o painel mostra "Demora excessiva". Os tempos-limite internos
  de cada consulta externa são definidos na TechSpec, compatíveis com esse teto.
- RNF-003 (privacidade): a localização segue BR-008 e BR-009; nenhum dado pessoal é persistido.
- RNF-004 (segurança da informação): respostas de erro do backend usam um formato único com código estável e
  mensagem em PT-BR, sem expor detalhes internos ou do provedor (BR-006).
- RNF-005 (acessibilidade): conforme `acessibilidade.md` — página em `pt-BR`; campo com rótulo visível;
  foco visível em cada elemento interativo; contraste WCAG AA nos dois temas; área de resultado anunciada
  como região dinâmica e marcada como ocupada durante o carregamento; erros anunciados como alerta; lista de
  cidades navegável por Tab e Enter; foco movido para o primeiro item da lista ou para o campo em erro de
  validação; animação de carregamento desativada quando o usuário prefere movimento reduzido.
- RNF-006 (renderização no servidor): a página renderiza no servidor sem consultar o backend e sem acessar
  APIs exclusivas do navegador; buscas e geolocalização só ocorrem no navegador.
- RNF-007 (responsividade): usável de 320 px de largura a desktop, sem rolagem horizontal; abaixo de 640 px
  o formulário empilha.
- RNF-008 (identidade visual): aparência exclusivamente com os tokens de `design-system.md` (tipografia
  monoespaçada, marcadores ASCII, sem sombras, sem azul/warning/success); prevalece sobre `DESIGN.md`
  (PRM-011).
- RNF-009 (testabilidade): cada comportamento é verificável por testes automatizados sem acesso à rede real,
  à geolocalização real ou ao relógio real (PRM-005).
- RNF-010 (não regressão): `GET /healthcheck` continua respondendo como hoje.
- RNF-011 (dependências): nenhuma dependência nova de backend ou frontend; único recurso externo no
  navegador é a fonte tipográfica já permitida.

## Experiência do usuário

**Fluxo principal.** O usuário abre o painel (estado Inicial, tema dia), digita "Recife" e pressiona Enter.
A área de resultado mostra o carregamento no formato do cartão final e é anunciada como ocupada. Chega o
resultado: "Recife, Pernambuco, Brasil", temperatura, sensação, umidade, vento com direção, condição em
texto, `[ DIA ]` ou `[ NOITE ]` e o horário da medição; o tema do painel acompanha dia/noite do local.

**Homônimos.** O usuário busca "São José": aparece a lista "São José — Santa Catarina, Brasil", "São José —
…", com o foco no primeiro item. Tab e Enter escolhem; o clima da cidade escolhida é exibido. Uma nova
busca descarta a lista.

**Localização.** Com geolocalização disponível, o usuário aciona "Usar minha localização"; o navegador pede
permissão. Concedida → carregamento → clima do local (identificação conforme PRM-016). Negada/indisponível →
mensagem informativa (ex.: "Não foi possível obter sua localização. Busque pelo nome da cidade.") e o campo
de busca segue disponível.

**Vazio e erro.** Entrada vazia ou curta → mensagem de validação junto ao campo, foco no campo, nenhuma
busca. Cidade inexistente → "Nenhuma cidade encontrada…" (sem retry; o usuário corrige o texto). Serviço fora
do ar ou lento → mensagem com marcador `[x]` e botão "Tentar novamente", que repete a mesma operação.

**Linguagem.** Toda a interface em PT-BR, tom direto; nomes de cidades exibidos como o provedor os
devolve em português, quando disponível (PRM-018). Ícones apenas como marcadores ASCII em texto.

**Acessibilidade.** Ver RNF-005.

## Impacto no contrato vivo

- clima: **NOVO** — consulta do clima atual por nome de cidade (com escolha entre homônimas), consulta por
  localização do navegador, estados de UI, regras de validação, mensagens e formato de erro.
  - Superfícies obrigatórias: **UI** (painel no frontend) **e API** (recurso de clima no backend) e
    **integração** com o provedor externo; uma entrega só de backend ou só de frontend é incompleta.
  - Requisitos relacionados: RF-001 a RF-010; BR-001 a BR-012; RNF-001 a RNF-011.
- Comportamento existente preservado (sem alteração de contrato): `GET /healthcheck` (RNF-010).
- Substituído (sem impacto `REMOVIDO`): o placeholder da página inicial do framework não é contrato; apenas
  dá lugar ao painel.

## Restrições e dependências

- Provedor externo Open-Meteo: busca de cidades por nome (Geocoding API) e condições atuais (Forecast API);
  gratuito, sem chave (PRM-005). A disponibilidade e a latência do provedor afetam diretamente RF-005/RNF-002.
- O provedor de busca de cidades **não oferece busca reversa** (coordenada → nome de cidade); isso limita
  RF-008 (ver PRM-016).
- Geolocalização depende de suporte do navegador, de contexto seguro (HTTPS — atendido pelo ambiente
  `https://test.cezar.dev/<VERSION>/`) e de consentimento do usuário.
- Restrições do projeto: `CLAUDE.md` e `.claude/rules/*` (sem CORS, sem prefixo fixo, sem dependências novas,
  arquivos de infraestrutura intocáveis, tokens de design obrigatórios).
- Ambiente de verificação manual: `compose.yaml` do projeto, não a sandbox global (PRM-008).
- Valores concretos (caminhos, parâmetros, campos, códigos de erro, chaves de configuração, tempos-limite,
  tabelas de tradução, nomes de arquivos/classes/seletores, textos exatos das mensagens) são definidos na
  TechSpec, fonte única conforme `prompt_1.md` (PRM-010).

## Fora de escopo

- Autenticação, banco de dados, cache, internacionalização, alternância de unidades e tema por preferência
  de cor do sistema (`restricoes.md`).
- Previsão para horas ou dias seguintes, histórico, gráficos, alertas meteorológicos e qualidade do ar.
- Autocompletar / sugestões enquanto digita (BR-005).
- Favoritos, cidades recentes ou qualquer memória entre visitas (inclui não lembrar a última cidade).
- Busca reversa de coordenadas por outro provedor que não a Open-Meteo.
- Deploy em produção (alvo `branch`; rota sem deploy).
- Alterações em infraestrutura (`compose.yaml`, Traefik, configuração de build/publicação).

## Premissas

Premissas herdadas do incremento (em `incremento.yaml`): PRM-001 a PRM-011 — em especial PRM-003 (endpoint
interno, sem contrato público), PRM-004 (localização só no navegador e não persistida), PRM-005 (sem chave,
sem rede real em testes), PRM-008 (ambiente via compose), PRM-010 (TechSpec define valores concretos) e
PRM-011 (prevalece `design-system.md`).

Novas premissas deste PRD:

- PRM-012: os campos exibidos são os de RF-003 (temperatura, sensação, umidade, vento + direção, condição,
  dia/noite, horário da medição); pressão, precipitação, nebulosidade e UV ficam de fora — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-013: limites do nome da cidade de 2 a 100 caracteres após `trim`, sem restrição de conjunto de
  caracteres (aceita acentos, hífen, apóstrofo, espaços internos) — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-014: homônimas são resolvidas por lista de escolha (até 10 opções), não pela primeira ocorrência nem
  por pedido de refinamento; apoiado por `acessibilidade.md` ("lista para escolher… mover o foco para o
  primeiro item") e pelas "linhas de lista" de `design-system.md` — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-015: a geolocalização **entra** neste incremento, mas por ação explícita ("Usar minha localização"),
  e não automaticamente ao abrir a página, para evitar pedido de permissão não solicitado — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-016: como a Open-Meteo não faz busca reversa, a consulta por localização exibe o clima **das
  coordenadas do usuário** identificado como "Sua localização" (sem nome de cidade), em vez de "sugerir a
  cidade" no campo de busca — confirmada pelo dev em 2026-10-20 (PRM-022); resposta à Pergunta 1.
- PRM-017: teto de 15 s para qualquer operação no estado "Carregando", incluindo a obtenção da posição pelo
  navegador — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-018: nomes de cidades e regiões são solicitados ao provedor em português e exibidos como devolvidos;
  condições do tempo e direção do vento são traduzidas pela aplicação para PT-BR — confirmada pelo dev em 2026-10-20 (PRM-022).
- PRM-019: antes do primeiro resultado o tema é "dia"; após um erro, ou na lista de escolha, o tema permanece o do último
  resultado exibido — confirmada pelo dev em 2026-10-20 (PRM-022).

### Rastreabilidade das perguntas do brief

| Pergunta do brief                                   | Resposta no PRD                                   |
|-----------------------------------------------------|---------------------------------------------------|
| Quais campos do clima atual o painel exibe?         | RF-003, BR-007, BR-011, BR-012, PRM-012           |
| Cidades homônimas: primeira, lista ou refinamento?  | RF-004, BR-002, PRM-014                           |
| Estados da UI obrigatórios e mensagens              | RF-005, RF-009, BR-006 (textos exatos: TechSpec)  |
| Validações do texto (tamanho, caracteres, `trim`)   | RF-002, BR-001, PRM-013                           |
| Geolocalização: entra? coordenada → cidade? negada? | RF-008, RF-009, BR-008–BR-010, PRM-015, PRM-016                 |
| Retry manual? Busca por submit ou enquanto digita?  | RF-006, BR-004, BR-005                            |
| Idioma de cidades e condições                       | BR-006, BR-007, PRM-018                           |

## Perguntas abertas

Respondidas pelo dev em 2026-10-20 no passo 04 (PRM-022): 1 → (a); 2 → só por ação do usuário;
3 → 15 s; 4 → PRM-012 e PRM-014 confirmadas. Mantidas abaixo como histórico da decisão.

1. **Geolocalização sem busca reversa** — o prompt fala em "sugerir a cidade automaticamente", mas a
   Open-Meteo não converte coordenada em nome de cidade. Opções: (a) mostrar o clima de "Sua localização"
   sem nome (PRM-016, padrão adotado); (b) mostrar também a coordenada arredondada; (c) adiar a
   geolocalização para outro incremento. Impacto se não respondida: segue (a); se o dev exigir nome de
   cidade, seria preciso outro provedor (fora de escopo) ou retirar RF-008/RF-009.
2. **Disparo da geolocalização** — automático ao abrir a página ou só por ação do usuário (PRM-015)?
   Impacto: o automático exibe pedido de permissão sem contexto e muda RF-008/BR-008 e os cenários de QA.
3. **Teto de espera** — 15 s (PRM-017) é aceitável? Impacto: define os tempos-limite da TechSpec e os
   cenários de "Demora excessiva".
4. **Campos e homônimos** — confirmar PRM-012 e PRM-014. Impacto: mudam o contrato de resposta do backend,
   os componentes de apresentação e os cenários de teste.
