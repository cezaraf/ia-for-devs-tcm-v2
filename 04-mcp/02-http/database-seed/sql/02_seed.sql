-- =========================================================
-- SQP - Dados de exemplo (seed)
-- =========================================================
BEGIN;

-- ---------- clientes ----------
INSERT INTO clientes (codigo, nome, documento, cidade, uf) VALUES
('C0001','Metalúrgica São Pedro LTDA','12.345.678/0001-90','Goiania','GO'),
('C0002','Indústria de Bebidas Anchieta','22.222.333/0001-44','Anapolis','GO'),
('C0003','Auto Peças Brasil S/A','33.999.888/0001-77','Aparecida de Goiania','GO'),
('C0004','Tech Components do Brasil','44.111.222/0001-55','São Paulo','SP'),
('C0005','Distribuidora Central','55.555.666/0001-22','Belo Horizonte','MG'),
('C0006','Construções Oeste','66.777.888/0001-11','Cuiaba','MT'),
('C0007','Hospital Santa Cruz','77.888.999/0001-33','Goiania','GO'),
('C0008','AgroTech Sementes','88.444.222/0001-09','Rio Verde','GO');

-- ---------- produtos ----------
INSERT INTO produtos (codigo, descricao, unidade, estoque, custo_padrao, e_ativo) VALUES
('P-1001','Parafuso sextavado M8x20','UN', 12000, 0.15, TRUE),
('P-1002','Parafuso sextavado M10x30','UN', 8500, 0.22, TRUE),
('P-1003','Arruela lisa M8','UN', 15000, 0.05, TRUE),
('P-1004','Arruela lisa M10','UN', 11000, 0.06, TRUE),
('P-1005','Porca autotravante M8','UN', 9800, 0.09, TRUE),
('P-1006','Porca autotravante M10','UN', 7200, 0.11, TRUE),
('P-2001','Conjunto flange 4"','UN', 320, 48.50, TRUE),
('P-2002','Conjunto flange 6"','UN', 180, 72.30, TRUE),
('P-2003','Eixo cardan 1m','UN', 60, 215.00, TRUE),
('P-3001','Caixa papelão 40x30x20','UN', 5400, 1.85, TRUE),
('P-3002','Bobina filme stretch 500m','UN', 220, 38.90, TRUE),
('P-4001','Suporte de parede L 200kg','UN', 150, 95.00, TRUE),
('P-4002','Cesto coletor 200L','UN', 80, 180.00, TRUE),
('P-5001','Resina epóxi 1kg','KG', 420, 52.00, TRUE),
('P-5002','Durepoxi 500g','UN', 650, 18.50, TRUE),
('P-9001','Produto descontinuado X','UN', 0, 0, FALSE);

-- ---------- centros_trabalho ----------
INSERT INTO centros_trabalho (codigo, nome, turno_padrao, capacidade_dia_h) VALUES
('CT-01','Corte','1TURNO', 8),
('CT-02','Dobras / Prensas','2TURNOS', 16),
('CT-03','Usinagem','2TURNOS', 16),
('CT-04','Solda','2TURNOS', 16),
('CT-05','Pintura','1TURNO', 8),
('CT-06','Montagem','2TURNOS', 16),
('CT-07','Embalagem','1TURNO', 8),
('CT-08','Qualidade','1TURNO', 8);

-- ---------- recursos ----------
INSERT INTO recursos (codigo, nome, tipo, centro_id, custo_hora) VALUES
('R-001','Operador Corte A','MÃO_DE_OBRA', 1, 28.00),
('R-002','Operador Corte B','MÃO_DE_OBRA', 1, 28.00),
('R-003','Operador Prensa 1','MÃO_DE_OBRA', 2, 32.00),
('R-004','Torneiro CNC 1','MÃO_DE_OBRA', 3, 45.00),
('R-005','Soldador 1','MÃO_DE_OBRA', 4, 38.00),
('R-006','Pintor 1','MÃO_DE_OBRA', 5, 30.00),
('R-007','Montador 1','MÃO_DE_OBRA', 6, 31.00),
('R-008','Montador 2','MÃO_DE_OBRA', 6, 31.00),
('R-009','Inspetor QA','MÃO_DE_OBRA', 8, 35.00),
('R-010','Serra Fita 1','EQUIPAMENTO', 1, 15.00),
('R-011','Prensa Hidráulica 1','EQUIPAMENTO', 2, 22.00),
('R-012','Torno CNC 1','EQUIPAMENTO', 3, 60.00),
('R-013','MIG 1','EQUIPAMENTO', 4, 18.00),
('R-014','Cabine Pintura 1','EQUIPAMENTO', 5, 25.00);

-- ---------- equipamentos ----------
INSERT INTO equipamentos (tag, nome, modelo, fabricante, centro_id, instalado_em, status) VALUES
('EQ-001','Serra Fita Industrial','SF-350','Bosch', 1, '2019-03-12','ATIVO'),
('EQ-002','Guilhotina 3m','GIL-3000','Urma', 1, '2020-06-01','ATIVO'),
('EQ-003','Prensa Hidráulica 200T','PH-200','Schuler', 2, '2018-11-20','ATIVO'),
('EQ-004','Prensa Mecânica 80T','PM-80','Mecanica BR', 2, '2021-01-15','ATIVO'),
('EQ-005','Torno CNC Horizontal','TN-450','Romi', 3, '2022-05-10','ATIVO'),
('EQ-006','Centro de Usinagem','CU-500','Mazak', 3, '2023-09-22','ATIVO'),
('EQ-007','MIG 400A','MIG-400','ESAB', 4, '2017-02-28','ATIVO'),
('EQ-008','TIG 300A','TIG-300','ESAB', 4, '2019-08-05','MANUTENCAO'),
('EQ-009','Cabine de Pintura','CAB-12','Nordson', 5, '2020-03-30','ATIVO'),
('EQ-010','Esteira Montagem 1','EST-1',' Bosch Rexroth', 6, '2021-07-14','ATIVO'),
('EQ-011','Esteira Montagem 2','EST-2','Bosch Rexroth', 6, '2023-02-09','ATIVO'),
('EQ-012','Empacotadora Semi-Aut','EMB-SA','Romaco', 7, '2019-12-01','ATIVO'),
('EQ-013','Banco de Testes 1','BT-1','NI', 8, '2022-10-10','ATIVO'),
('EQ-014','Calibrador de Torque','CAL-T','Atlas', 8, '2020-01-05','ATIVO');

-- ---------- operacoes ----------
INSERT INTO operacoes (codigo, descricao, centro_id, tempo_setup_min, tempo_unit_min) VALUES
('OP-10','Corte de matéria-prima',1, 15, 0.50),
('OP-20','Prensagem / Dobras',2, 25, 1.20),
('OP-30','Tornejamento CNC',3, 40, 3.50),
('OP-40','Furação CNC',3, 20, 1.00),
('OP-50','Soldagem MIG',4, 30, 4.00),
('OP-60','Soldagem TIG',4, 35, 5.00),
('OP-70','Pintura Eletrostática',5, 45, 2.00),
('OP-80','Montagem',6, 15, 6.00),
('OP-90','Testes / Qualidade',8, 10, 2.00),
('OP-95','Embalagem',7, 5, 0.50);

-- ---------- ordens ----------
INSERT INTO ordens (numero, cliente_id, produto_id, quantidade, qt_produzida, status, prioridade, emissao, entrega) VALUES
('OP-2024-0001', 1, 7,  300,  300, 'CONCLUIDA', 2, '2024-01-08','2024-01-20'),
('OP-2024-0002', 1, 8,  150,  150, 'CONCLUIDA', 2, '2024-01-12','2024-01-25'),
('OP-2024-0003', 2, 9,   60,   60, 'CONCLUIDA', 1, '2024-02-02','2024-02-10'),
('OP-2024-0004', 3, 11, 400,  300, 'EM_PRODUCAO', 1, '2024-02-15','2024-02-28'),
('OP-2024-0005', 4, 12,  80,   80, 'CONCLUIDA', 3, '2024-03-01','2024-03-15'),
('OP-2024-0006', 5, 13,  40,   40, 'CONCLUIDA', 3, '2024-03-10','2024-03-25'),
('OP-2024-0007', 2, 9,  120,   40, 'EM_PRODUCAO', 2, '2024-04-01','2024-04-20'),
('OP-2024-0008', 6, 11, 600,    0, 'PAUSADA', 2, '2024-04-05','2024-05-10'),
('OP-2024-0009', 7, 12,  50,    0, 'ABERTA', 4, '2024-04-22','2024-05-30'),
('OP-2024-0010', 8, 14,  20,    0, 'CANCELADA', 5, '2024-05-01','2024-05-15'),
('OP-2024-0011', 1, 7,  500,    0, 'ABERTA', 2, '2024-05-02','2024-05-20'),
('OP-2024-0012', 3, 8,  100,   50, 'EM_PRODUCAO', 3, '2024-05-05','2024-05-25'),
('OP-2024-0013', 4, 11, 250,    0, 'ABERTA', 2, '2024-05-08','2024-05-30'),
('OP-2024-0014', 5, 13,  30,    0, 'ABERTA', 4, '2024-05-10','2024-06-01'),
('OP-2024-0015', 2, 9,   80,    0, 'ABERTA', 1, '2024-05-12','2024-06-10'),
('OP-2024-0016', 6, 7,  200,    0, 'ABERTA', 3, '2024-05-15','2024-06-15');

-- ---------- ordem_itens ----------
INSERT INTO ordem_itens (ordem_id, produto_id, tipo, quantidade, unidade) VALUES
(1, 1, 'MAT', 1200, 'UN'),
(1, 3, 'MAT', 1200, 'UN'),
(1, 5, 'MAT', 1200, 'UN'),
(1, 10, 'EMB', 300, 'UN'),
(2, 2, 'MAT', 600, 'UN'),
(2, 4, 'MAT', 600, 'UN'),
(2, 6, 'MAT', 600, 'UN'),
(2, 10, 'EMB', 150, 'UN'),
(3, 3, 'MAT', 240, 'UN'),
(3, 5, 'MAT', 240, 'UN'),
(3, 11, 'EMB', 60, 'UN'),
(4, 1, 'MAT', 1600, 'UN'),
(4, 5, 'MAT', 1600, 'UN'),
(4, 10, 'EMB', 400, 'UN'),
(5, 2, 'MAT', 320, 'UN'),
(5, 4, 'MAT', 320, 'UN'),
(5, 6, 'MAT', 320, 'UN'),
(6, 14, 'MAT', 60, 'UN'),
(6, 11, 'EMB', 40, 'UN'),
(7, 3, 'MAT', 480, 'UN'),
(7, 5, 'MAT', 480, 'UN'),
(7, 11, 'EMB', 120, 'UN'),
(8, 1, 'MAT', 2400, 'UN'),
(8, 3, 'MAT', 2400, 'UN'),
(8, 5, 'MAT', 2400, 'UN'),
(8, 10, 'EMB', 600, 'UN'),
(11, 1, 'MAT', 2000, 'UN'),
(11, 3, 'MAT', 2000, 'UN'),
(11, 5, 'MAT', 2000, 'UN'),
(11, 10, 'EMB', 500, 'UN'),
(12, 2, 'MAT', 400, 'UN'),
(12, 4, 'MAT', 400, 'UN'),
(12, 6, 'MAT', 400, 'UN'),
(12, 10, 'EMB', 100, 'UN'),
(13, 1, 'MAT', 1000, 'UN'),
(13, 5, 'MAT', 1000, 'UN'),
(13, 10, 'EMB', 250, 'UN'),
(15, 3, 'MAT', 320, 'UN'),
(15, 5, 'MAT', 320, 'UN'),
(15, 11, 'EMB', 80, 'UN'),
(16, 2, 'MAT', 800, 'UN'),
(16, 4, 'MAT', 800, 'UN'),
(16, 6, 'MAT', 800, 'UN'),
(16, 10, 'EMB', 200, 'UN');

-- ---------- ordem_operacoes ----------
INSERT INTO ordem_operacoes (ordem_id, operacao_id, sequencia, recurso_id, inicio_prev, fim_prev, inicio_real, fim_real, qty_apontada, status) VALUES
(1, 1, 10, 1,  '2024-01-08 07:00','2024-01-08 12:00','2024-01-08 07:05','2024-01-08 11:50', 300, 'CONCLUIDA'),
(1, 2, 20, 3,  '2024-01-09 07:00','2024-01-09 18:00','2024-01-09 07:10','2024-01-09 17:30', 300, 'CONCLUIDA'),
(1, 5, 30, 5,  '2024-01-10 07:00','2024-01-10 17:00','2024-01-10 07:00','2024-01-10 16:40', 300, 'CONCLUIDA'),
(1, 8, 40, 7,  '2024-01-11 07:00','2024-01-12 18:00','2024-01-11 07:00','2024-01-12 17:20', 300, 'CONCLUIDA'),
(1, 9, 50, 9,  '2024-01-15 07:00','2024-01-15 12:00','2024-01-15 07:00','2024-01-15 11:40', 300, 'CONCLUIDA'),
(1,10, 60, NULL,'2024-01-16 07:00','2024-01-16 12:00','2024-01-16 07:00','2024-01-16 11:30', 300, 'CONCLUIDA'),
(2, 1, 10, 1,  '2024-01-12 07:00','2024-01-12 12:00','2024-01-12 07:00','2024-01-12 12:10', 150, 'CONCLUIDA'),
(2, 2, 20, 3,  '2024-01-13 07:00','2024-01-13 16:00','2024-01-13 07:00','2024-01-13 15:50', 150, 'CONCLUIDA'),
(2, 8, 30, 7,  '2024-01-16 07:00','2024-01-17 17:00','2024-01-16 07:00','2024-01-17 16:40', 150, 'CONCLUIDA'),
(2,10, 40, NULL,'2024-01-18 07:00','2024-01-18 12:00','2024-01-18 07:00','2024-01-18 11:50', 150, 'CONCLUIDA'),
(3, 1, 10, 2,  '2024-02-02 07:00','2024-02-02 11:00','2024-02-02 07:00','2024-02-02 10:50', 60, 'CONCLUIDA'),
(3, 3, 20, 4,  '2024-02-03 07:00','2024-02-03 19:00','2024-02-03 07:00','2024-02-03 18:40', 60, 'CONCLUIDA'),
(3, 5, 30, 5,  '2024-02-05 07:00','2024-02-05 12:00','2024-02-05 07:00','2024-02-05 11:30', 60, 'CONCLUIDA'),
(3, 8, 40, 8,  '2024-02-06 07:00','2024-02-06 17:00','2024-02-06 07:00','2024-02-06 16:50', 60, 'CONCLUIDA'),
(4, 1, 10, 1,  '2024-02-15 07:00','2024-02-15 17:00','2024-02-15 07:00','2024-02-15 17:10', 300, 'CONCLUIDA'),
(4, 2, 20, 3,  '2024-02-16 07:00','2024-02-18 17:00','2024-02-16 07:00', NULL, 300, 'EM_EXECUCAO'),
(4, 5, 30, 5,  '2024-02-19 07:00','2024-02-19 17:00', NULL, NULL, 0, 'PENDENTE'),
(4, 8, 40, 7,  '2024-02-20 07:00','2024-02-21 17:00', NULL, NULL, 0, 'PENDENTE'),
(7, 1, 10, 2,  '2024-04-01 07:00','2024-04-01 13:00','2024-04-01 07:00','2024-04-01 13:10', 40, 'CONCLUIDA'),
(7, 3, 20, 4,  '2024-04-02 07:00','2024-04-02 17:00','2024-04-02 07:00', NULL, 40, 'EM_EXECUCAO'),
(7, 5, 30, 5,  '2024-04-03 07:00','2024-04-03 12:00', NULL, NULL, 0, 'PENDENTE'),
(7, 8, 40, 8,  '2024-04-04 07:00','2024-04-05 17:00', NULL, NULL, 0, 'PENDENTE'),
(8, 1, 10, 1,  '2024-04-05 07:00','2024-04-05 17:00','2024-04-05 07:00','2024-04-05 17:00', 0, 'CONCLUIDA'),
(8, 2, 20, 3,  '2024-04-08 07:00','2024-04-10 17:00', NULL, NULL, 0, 'PENDENTE'),
(12, 1, 10, 1, '2024-05-05 07:00','2024-05-05 12:00','2024-05-05 07:00','2024-05-05 12:00', 50, 'CONCLUIDA'),
(12, 2, 20, 3, '2024-05-06 07:00','2024-05-07 17:00','2024-05-06 07:00', NULL, 50, 'EM_EXECUCAO');

-- ---------- notas ----------
INSERT INTO notas (numero, serie, tipo, status, cliente_id, parceiro_doc, emissao, valor_total, ordem_id) VALUES
('NF-100001','001','SAIDA','EMITIDA', 1, '12.345.678/0001-90','2024-01-22', 14610.00, 1),
('NF-100002','001','SAIDA','EMITIDA', 1, '12.345.678/0001-90','2024-01-26', 10845.00, 2),
('NF-100003','001','SAIDA','EMITIDA', 2, '22.222.333/0001-44','2024-02-12', 12900.00, 3),
('NF-100004','001','SAIDA','RECEBIDA', 3, '33.999.888/0001-77','2024-03-01', 10040.00, 5),
('NF-100005','001','SAIDA','EMITIDA', 5, '55.555.666/0001-22','2024-03-26', 7200.00, 6),
('NF-100006','001','ENTRADA','RECEBIDA', NULL, '88.444.222/0001-09','2024-04-02', 26250.00, NULL),
('NF-100007','001','SAIDA','CANCELADA', 4, '44.111.222/0001-55','2024-04-10', 7600.00, NULL),
('NF-100008','001','SAIDA','EMITIDA', 6, '66.777.888/0001-11','2024-04-12', 21840.00, NULL),
('NF-100009','001','ENTRADA','RECEBIDA', NULL, '11.111.111/0001-00','2024-04-15', 18400.00, NULL),
('NF-100010','001','SAIDA','EMITIDA', 3, '33.999.888/0001-77','2024-04-20', 9120.00, NULL),
('NF-100011','001','SAIDA','EMITIDA', 7, '77.888.999/0001-33','2024-04-25', 4800.00, NULL),
('NF-100012','001','SAIDA','EMITIDA', 1, '12.345.678/0001-90','2024-05-02', 24250.00, NULL),
('NF-100013','001','ENTRADA','RECEBIDA', NULL, '88.444.222/0001-09','2024-05-04', 20800.00, NULL),
('NF-100014','001','SAIDA','EMITIDA', 2, '22.222.333/0001-44','2024-05-08', 17200.00, NULL),
('NF-100015','001','SAIDA','EMITIDA', 5, '55.555.666/0001-22','2024-05-10', 5400.00, NULL),
('NF-100016','001','SAIDA','EMITIDA', 6, '66.777.888/0001-11','2024-05-12', 9600.00, NULL),
('NF-100017','001','SAIDA','EMITIDA', 8, '88.444.222/0001-09','2024-05-15', 36400.00, NULL),
('NF-100018','001','SAIDA','EMITIDA', 4, '44.111.222/0001-55','2024-05-18', 22600.00, NULL),
('NF-100019','001','SAIDA','CANCELADA', 3, '33.999.888/0001-77','2024-05-20', 13400.00, NULL),
('NF-100020','001','SAIDA','EMITIDA', 1, '12.345.678/0001-90','2024-05-22', 31980.00, NULL);

-- ---------- nota_itens ----------
INSERT INTO nota_itens (nota_id, produto_id, quantidade, valor_unit, valor_total) VALUES
(1, 7, 300, 48.50, 14550.00),
(1, 10,  3, 20.00,   60.00),
(2, 8, 150, 72.30, 10845.00),
(3, 9,  60, 215.00, 12900.00),
(4, 11, 200, 50.20, 10040.00),
(5, 13, 40, 180.00, 7200.00),
(6, 15, 500, 52.50, 26250.00),
(8, 11, 1200, 18.20, 21840.00),
(9, 1, 10000, 1.84, 18400.00),
(10, 12, 80, 95.00, 7600.00),
(11, 13, 80, 60.00, 4800.00),
(12, 7, 500, 48.50, 24250.00),
(13, 16, 400, 52.00, 20800.00),
(14, 9, 80, 215.00, 17200.00),
(15, 14, 100, 54.00, 5400.00),
(16, 11, 400, 24.00, 9600.00),
(17, 12, 350, 95.00, 33250.00),
(17, 13, 17, 185.29, 3150.00),
(18, 11, 1000, 22.60, 22600.00),
(19, 12, 140, 95.00, 13300.00),
(19, 13, 0.7071, 1414.21, 1000.00),
(20, 7, 660, 48.50, 32010.00);

COMMIT;