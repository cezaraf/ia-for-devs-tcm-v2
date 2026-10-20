-- =========================================================
-- SQP - Schema de exemplo para o MCP read-only
-- Tabelas no estilo ERP de manufatura (ordens de produção,
-- notas fiscais, equipamentos, operações, centros de trabalho,
-- recursos, clientes e produtos).
-- =========================================================

BEGIN;

-- ---------- Tipos / enums ----------
CREATE TYPE ordem_status AS ENUM ('ABERTA','EM_PRODUCAO','PAUSADA','CONCLUIDA','CANCELADA');
CREATE TYPE nota_status  AS ENUM ('EMITIDA','CANCELADA','RECEBIDA');
CREATE TYPE nota_tipo    AS ENUM ('SAIDA','ENTRADA');
CREATE TYPE op_status    AS ENUM ('PENDENTE','EM_EXECUCAO','CONCLUIDA','RETRABALHO','CANCELADA');

-- ---------- clientes ----------
CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(15)  UNIQUE NOT NULL,
    nome         VARCHAR(120) NOT NULL,
    documento    VARCHAR(18)  NOT NULL,
    cidade       VARCHAR(60),
    uf           VARCHAR(2),
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- produtos ----------
CREATE TABLE produtos (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(20) UNIQUE NOT NULL,
    descricao    VARCHAR(200) NOT NULL,
    unidade      VARCHAR(3) NOT NULL DEFAULT 'UN',
    estoque     NUMERIC(14,3) NOT NULL DEFAULT 0,
    custo_padrao NUMERIC(14,4) NOT NULL DEFAULT 0,
    e_ativo      BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- centros_trabalho ----------
CREATE TABLE centros_trabalho (
    id          BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(10) UNIQUE NOT NULL,
    nome        VARCHAR(60) NOT NULL,
    turno_padrao VARCHAR(10) NOT NULL DEFAULT '1TURNO',
    capacidade_dia_h INTEGER NOT NULL DEFAULT 8
);

-- ---------- recursos (mão de obra / máquina) ----------
CREATE TABLE recursos (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(10) UNIQUE NOT NULL,
    nome        VARCHAR(60) NOT NULL,
    tipo         VARCHAR(20) NOT NULL,
    centro_id    BIGINT NOT NULL REFERENCES centros_trabalho(id),
    custo_hora  NUMERIC(10,2) NOT NULL DEFAULT 0
);

-- ---------- equipamentos ----------
CREATE TABLE equipamentos (
    id           BIGSERIAL PRIMARY KEY,
    tag          VARCHAR(15) UNIQUE NOT NULL,
    nome         VARCHAR(80) NOT NULL,
    modelo       VARCHAR(60),
    fabricante   VARCHAR(60),
    centro_id    BIGINT REFERENCES centros_trabalho(id),
    instalado_em DATE,
    status       VARCHAR(15) NOT NULL DEFAULT 'ATIVO'
);

-- ---------- ordens (ordens de produção) ----------
CREATE TABLE ordens (
    id           BIGSERIAL PRIMARY KEY,
    numero       VARCHAR(15) UNIQUE NOT NULL,
    cliente_id   BIGINT NOT NULL REFERENCES clientes(id),
    produto_id   BIGINT NOT NULL REFERENCES produtos(id),
    quantidade   NUMERIC(14,3) NOT NULL DEFAULT 1,
    qt_produzida NUMERIC(14,3) NOT NULL DEFAULT 0,
    status       ordem_status NOT NULL DEFAULT 'ABERTA',
    prioridade   INTEGER NOT NULL DEFAULT 3,
    emissao      DATE NOT NULL DEFAULT CURRENT_DATE,
    entrega      DATE,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- ordem_itens ----------
CREATE TABLE ordem_itens (
    id            BIGSERIAL PRIMARY KEY,
    ordem_id      BIGINT NOT NULL REFERENCES ordens(id) ON DELETE CASCADE,
    produto_id    BIGINT NOT NULL REFERENCES produtos(id),
    tipo          VARCHAR(3) NOT NULL,
    quantidade    NUMERIC(14,3) NOT NULL,
    unidade       VARCHAR(3) NOT NULL DEFAULT 'UN'
);

-- ---------- operacoes (roteiro) ----------
CREATE TABLE operacoes (
    id            BIGSERIAL PRIMARY KEY,
    codigo        VARCHAR(10) NOT NULL,
    descricao     VARCHAR(120) NOT NULL,
    centro_id     BIGINT NOT NULL REFERENCES centros_trabalho(id),
    tempo_setup_min INTEGER NOT NULL DEFAULT 0,
    tempo_unit_min  NUMERIC(10,2) NOT NULL DEFAULT 0
);

-- ---------- ordem_operacoes (apontamentos) ----------
CREATE TABLE ordem_operacoes (
    id              BIGSERIAL PRIMARY KEY,
    ordem_id        BIGINT NOT NULL REFERENCES ordens(id) ON DELETE CASCADE,
    operacao_id     BIGINT NOT NULL REFERENCES operacoes(id),
    sequencia        INTEGER NOT NULL,
    recurso_id      BIGINT REFERENCES recursos(id),
    inicio_prev     TIMESTAMPTZ,
    fim_prev        TIMESTAMPTZ,
    inicio_real     TIMESTAMPTZ,
    fim_real        TIMESTAMPTZ,
    qty_apontada    NUMERIC(14,3) NOT NULL DEFAULT 0,
    status          op_status NOT NULL DEFAULT 'PENDENTE'
);

-- ---------- notas (fiscais) ----------
CREATE TABLE notas (
    id             BIGSERIAL PRIMARY KEY,
    numero         VARCHAR(20) UNIQUE NOT NULL,
    serie          VARCHAR(3) NOT NULL DEFAULT '001',
    tipo           nota_tipo NOT NULL,
    status         nota_status NOT NULL DEFAULT 'EMITIDA',
    cliente_id     BIGINT REFERENCES clientes(id),
    parceiro_doc   VARCHAR(18),
    emissao        DATE NOT NULL DEFAULT CURRENT_DATE,
    valor_total    NUMERIC(14,2) NOT NULL DEFAULT 0,
    ordem_id       BIGINT REFERENCES ordens(id)
);

-- ---------- nota_itens ----------
CREATE TABLE nota_itens (
    id             BIGSERIAL PRIMARY KEY,
    nota_id        BIGINT NOT NULL REFERENCES notas(id) ON DELETE CASCADE,
    produto_id     BIGINT NOT NULL REFERENCES produtos(id),
    quantidade     NUMERIC(14,3) NOT NULL,
    valor_unit     NUMERIC(14,4) NOT NULL,
    valor_total   NUMERIC(14,2) NOT NULL
);

-- ---------- comentários (metadados para consumo por LLM/MCP) ----------

COMMENT ON TYPE ordem_status IS 'Status da ordem de produção: ABERTA (criada, não iniciada), EM_PRODUCAO (em execução), PAUSADA (interrompida temporariamente), CONCLUIDA (finalizada), CANCELADA';
COMMENT ON TYPE nota_status  IS 'Status da nota fiscal: EMITIDA, CANCELADA, RECEBIDA (apenas notas de entrada)';
COMMENT ON TYPE nota_tipo    IS 'Tipo da nota fiscal: SAIDA (venda/faturamento para cliente), ENTRADA (recebimento de fornecedor)';
COMMENT ON TYPE op_status    IS 'Status da operação apontada: PENDENTE, EM_EXECUCAO, CONCLUIDA, RETRABALHO (precisa refazer), CANCELADA';

COMMENT ON TABLE clientes IS 'Cadastro de clientes do ERP';
COMMENT ON COLUMN clientes.codigo    IS 'Código único do cliente no ERP (ex.: CLI-0001)';
COMMENT ON COLUMN clientes.nome      IS 'Razão social ou nome do cliente';
COMMENT ON COLUMN clientes.documento IS 'CNPJ ou CPF do cliente (com máscara)';
COMMENT ON COLUMN clientes.cidade    IS 'Cidade do endereço principal';
COMMENT ON COLUMN clientes.uf        IS 'Unidade federativa (sigla de 2 letras, ex.: SP, MG)';
COMMENT ON COLUMN clientes.criado_em IS 'Data/hora de criação do registro';

COMMENT ON TABLE produtos IS 'Cadastro de produtos (itens fabricados e matérias-primas)';
COMMENT ON COLUMN produtos.codigo       IS 'Código único do produto (SKU)';
COMMENT ON COLUMN produtos.descricao    IS 'Descrição comercial do produto';
COMMENT ON COLUMN produtos.unidade      IS 'Unidade de medida padrão (UN, KG, M, L etc.)';
COMMENT ON COLUMN produtos.estoque      IS 'Saldo atual em estoque, na unidade padrão do produto';
COMMENT ON COLUMN produtos.custo_padrao IS 'Custo padrão unitário usado em valorização (moeda local)';
COMMENT ON COLUMN produtos.e_ativo      IS 'TRUE = produto ativo/vendável; FALSE = descontinuado';

COMMENT ON TABLE centros_trabalho IS 'Centros de trabalho da fábrica (agrupam recursos e equipamentos)';
COMMENT ON COLUMN centros_trabalho.codigo           IS 'Código único do centro de trabalho (ex.: CT-USIN)';
COMMENT ON COLUMN centros_trabalho.nome             IS 'Nome descritivo do centro (ex.: Usinagem, Montagem)';
COMMENT ON COLUMN centros_trabalho.turno_padrao     IS 'Turno padrão de operação (ex.: 1TURNO, 2TURNOS)';
COMMENT ON COLUMN centros_trabalho.capacidade_dia_h IS 'Capacidade produtiva diária em horas';

COMMENT ON TABLE recursos IS 'Recursos produtivos: mão de obra ou máquina, vinculados a um centro de trabalho';
COMMENT ON COLUMN recursos.codigo     IS 'Código único do recurso';
COMMENT ON COLUMN recursos.nome       IS 'Nome do recurso (operador ou máquina)';
COMMENT ON COLUMN recursos.tipo       IS 'Tipo do recurso: MAO_DE_OBRA ou MAQUINA';
COMMENT ON COLUMN recursos.centro_id  IS 'FK para centros_trabalho — centro ao qual o recurso pertence';
COMMENT ON COLUMN recursos.custo_hora IS 'Custo por hora do recurso (moeda local)';

COMMENT ON TABLE equipamentos IS 'Ativos/equipamentos industriais instalados na fábrica';
COMMENT ON COLUMN equipamentos.tag          IS 'TAG única de identificação do equipamento (padrão de manutenção)';
COMMENT ON COLUMN equipamentos.nome         IS 'Nome do equipamento';
COMMENT ON COLUMN equipamentos.modelo       IS 'Modelo informado pelo fabricante';
COMMENT ON COLUMN equipamentos.fabricante   IS 'Fabricante do equipamento';
COMMENT ON COLUMN equipamentos.centro_id    IS 'FK para centros_trabalho — onde o equipamento está instalado (pode ser NULL)';
COMMENT ON COLUMN equipamentos.instalado_em IS 'Data de instalação do equipamento';
COMMENT ON COLUMN equipamentos.status       IS 'Situação operacional: ATIVO, MANUTENCAO, INATIVO';

COMMENT ON TABLE ordens IS 'Ordens de produção (OP): o que produzir, para qual cliente, quanto e quando';
COMMENT ON COLUMN ordens.numero       IS 'Número único da ordem de produção (ex.: OP-2024-0001)';
COMMENT ON COLUMN ordens.cliente_id   IS 'FK para clientes — cliente solicitante da ordem';
COMMENT ON COLUMN ordens.produto_id   IS 'FK para produtos — produto acabado a ser produzido';
COMMENT ON COLUMN ordens.quantidade   IS 'Quantidade planejada a produzir';
COMMENT ON COLUMN ordens.qt_produzida IS 'Quantidade já produzida/apontada até o momento';
COMMENT ON COLUMN ordens.status       IS 'Status da ordem (enum ordem_status)';
COMMENT ON COLUMN ordens.prioridade   IS 'Prioridade de 1 (mais urgente) a 5 (menos urgente); padrão 3';
COMMENT ON COLUMN ordens.emissao      IS 'Data de emissão/criação da ordem';
COMMENT ON COLUMN ordens.entrega      IS 'Data prometida de entrega ao cliente';
COMMENT ON COLUMN ordens.criado_em    IS 'Data/hora de criação do registro';

COMMENT ON TABLE ordem_itens IS 'Itens/materiais da ordem de produção (lista de materiais consumidos ou produzidos)';
COMMENT ON COLUMN ordem_itens.ordem_id   IS 'FK para ordens — ordem à qual o item pertence';
COMMENT ON COLUMN ordem_itens.produto_id IS 'FK para produtos — material ou produto do item';
COMMENT ON COLUMN ordem_itens.tipo       IS 'Tipo do item: MP (matéria-prima consumida) ou PA (produto acabado gerado)';
COMMENT ON COLUMN ordem_itens.quantidade IS 'Quantidade do item na ordem';
COMMENT ON COLUMN ordem_itens.unidade    IS 'Unidade de medida do item (UN, KG etc.)';

COMMENT ON TABLE operacoes IS 'Catálogo de operações do roteiro de fabricação (ex.: corte, usinagem, montagem)';
COMMENT ON COLUMN operacoes.codigo          IS 'Código da operação (ex.: OP10, OP20)';
COMMENT ON COLUMN operacoes.descricao       IS 'Descrição da operação';
COMMENT ON COLUMN operacoes.centro_id       IS 'FK para centros_trabalho — centro onde a operação é executada';
COMMENT ON COLUMN operacoes.tempo_setup_min IS 'Tempo de preparação/setup em minutos (fixo por lote)';
COMMENT ON COLUMN operacoes.tempo_unit_min  IS 'Tempo de execução por unidade produzida, em minutos';

COMMENT ON TABLE ordem_operacoes IS 'Apontamentos: execução das operações do roteiro dentro de cada ordem de produção';
COMMENT ON COLUMN ordem_operacoes.ordem_id     IS 'FK para ordens — ordem à qual o apontamento pertence';
COMMENT ON COLUMN ordem_operacoes.operacao_id  IS 'FK para operacoes — operação do roteiro executada';
COMMENT ON COLUMN ordem_operacoes.sequencia    IS 'Ordem de execução da operação dentro da OP (10, 20, 30...)';
COMMENT ON COLUMN ordem_operacoes.recurso_id   IS 'FK para recursos — recurso alocado (NULL = não alocado)';
COMMENT ON COLUMN ordem_operacoes.inicio_prev  IS 'Início previsto/planejado da operação';
COMMENT ON COLUMN ordem_operacoes.fim_prev     IS 'Fim previsto/planejado da operação';
COMMENT ON COLUMN ordem_operacoes.inicio_real  IS 'Início real apontado (NULL = ainda não iniciada)';
COMMENT ON COLUMN ordem_operacoes.fim_real     IS 'Fim real apontado (NULL = ainda não concluída)';
COMMENT ON COLUMN ordem_operacoes.qty_apontada IS 'Quantidade produzida apontada nesta operação';
COMMENT ON COLUMN ordem_operacoes.status       IS 'Status da operação (enum op_status)';

COMMENT ON TABLE notas IS 'Notas fiscais de saída (faturamento a clientes) e entrada (recebimento de fornecedores)';
COMMENT ON COLUMN notas.numero       IS 'Número único da nota fiscal';
COMMENT ON COLUMN notas.serie        IS 'Série da nota fiscal (padrão 001)';
COMMENT ON COLUMN notas.tipo         IS 'SAIDA = venda para cliente; ENTRADA = recebimento de fornecedor';
COMMENT ON COLUMN notas.status       IS 'Status da nota (enum nota_status)';
COMMENT ON COLUMN notas.cliente_id   IS 'FK para clientes — preenchido em notas de SAIDA; NULL em notas de ENTRADA';
COMMENT ON COLUMN notas.parceiro_doc IS 'CNPJ/CPF do parceiro — usado em notas de ENTRADA (fornecedor)';
COMMENT ON COLUMN notas.emissao      IS 'Data de emissão da nota';
COMMENT ON COLUMN notas.valor_total  IS 'Valor total da nota (soma dos itens), em moeda local';
COMMENT ON COLUMN notas.ordem_id     IS 'FK para ordens — ordem de produção que originou a nota (NULL se avulsa)';

COMMENT ON TABLE nota_itens IS 'Itens (linhas) das notas fiscais';
COMMENT ON COLUMN nota_itens.nota_id     IS 'FK para notas — nota à qual o item pertence';
COMMENT ON COLUMN nota_itens.produto_id  IS 'FK para produtos — produto faturado/recebido';
COMMENT ON COLUMN nota_itens.quantidade  IS 'Quantidade do item';
COMMENT ON COLUMN nota_itens.valor_unit  IS 'Valor unitário do item';
COMMENT ON COLUMN nota_itens.valor_total IS 'Valor total do item (quantidade × valor_unit)';

-- ---------- usuario read-only ----------
DO $$
BEGIN
    EXECUTE format('CREATE ROLE sqp_ro LOGIN PASSWORD %L', 'sqp_ro_pwd');
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;

GRANT CONNECT ON DATABASE sqp TO sqp_ro;
GRANT USAGE ON SCHEMA public TO sqp_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sqp_ro;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO sqp_ro;

COMMIT;