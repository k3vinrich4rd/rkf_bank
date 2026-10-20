-- Contas da EQUIPE: as duas que existem sempre que a API sobe (perfil padrao e perfil equipe).
--
--   Felipe Santana (idealizador, PO & QA e primeiro cliente) -> conta ORIGEM  0001 / 100001, saldo inicial R$ 1.000,00
--   Renan Barros   (idealizador, frontend)                   -> conta DESTINO 0001 / 100002, saldo inicial R$ 500,00
--
-- SEM senha aqui, de proposito: o repositorio e publico. O login das duas contas e criado na subida pelo
-- CriadorAcessoEquipe com a EQUIPE_SENHA do .env (o mesmo motivo do CriadorAdmin). Sem EQUIPE_SENHA, as contas
-- existem, mas ninguem entra nelas. Dados de acesso: guia privado da equipe (.equipe/, fora do Git).
--
-- Por que "afterMigrate" e nao V7__: e um callback do Flyway. Roda depois de TODA execucao do migrate, ou seja,
-- a cada subida da API, mesmo sem migracao nova, e nao entra no flyway_schema_history. Apagou as contas na mao?
-- Na proxima subida elas voltam.
-- Cada INSERT e idempotente (so insere o que falta, pelo id E pela chave natural: CPF, e-mail, numero da conta).
-- Rodar 1 ou 100 vezes da o mesmo resultado, e saldo ja movimentado nunca e resetado.
-- Se alguem cadastrou o mesmo CPF pela API, o cliente da equipe e pulado, e os INSERTs seguintes (conta,
-- lancamento) tambem, porque conferem se o "pai" com o id fixo existe. Nada quebra a subida.
--
-- Onde roda: perfil padrao (MySQL) e perfil equipe (H2). NAO roda no visitante, no test nem em producao
-- (FLYWAY_LOCATIONS=classpath:db/migration).
--
-- Ids fixos (UUID "5eed..." = seed) para o guia da equipe usar os mesmos ids em qualquer maquina.
-- X'...' e o literal binario aceito pelo MySQL e pelo H2; o UUID vira os 16 bytes na ordem em que o Hibernate le.
-- CPFs ficticios, validos pelos digitos verificadores e diferentes dos usados nos testes automatizados.
-- Enderecos com CEP real de Osasco (o RKF Bank nasceu la); numero e telefone ficticios.

-- ---------- Felipe Santana: conta de ORIGEM ----------

INSERT INTO tb_endereco (id, cep, logradouro, bairro, cidade, uf, ibge, numero, complemento)
SELECT X'5eed0000000040008000000000000011', '06013-000', 'Rua Antônio Agu', 'Centro', 'Osasco', 'SP', '3534401', '1001', NULL
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_endereco WHERE id = X'5eed0000000040008000000000000011')
  AND NOT EXISTS (SELECT 1 FROM tb_cliente WHERE cpf = '12345678909');

INSERT INTO tb_cliente (id, nome_completo, cpf, email, telefone, endereco_id)
SELECT X'5eed0000000040008000000000000012', 'Felipe Santana', '12345678909', 'felipe.santana@rkfbank.test', '11987650001',
       X'5eed0000000040008000000000000011'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente
                  WHERE id = X'5eed0000000040008000000000000012' OR cpf = '12345678909' OR email = 'felipe.santana@rkfbank.test')
  AND EXISTS (SELECT 1 FROM tb_endereco WHERE id = X'5eed0000000040008000000000000011');

INSERT INTO tb_conta (id, numero_conta, agencia, tipo_conta_enum, saldo, ativa, versao, cliente_id)
SELECT X'5eed0000000040008000000000000014', '100001', '0001', 'CORRENTE', 1000.00, TRUE, 0, X'5eed0000000040008000000000000012'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_conta WHERE id = X'5eed0000000040008000000000000014' OR numero_conta = '100001')
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = X'5eed0000000040008000000000000012');

-- O saldo inicial tem um lancamento que o explica: saldo e extrato sempre batem.
INSERT INTO tb_lancamento (id, tipo_lancamento_enum, valor, conta_origem_id, conta_destino_id, descricao, data_hora)
SELECT X'5eed0000000040008000000000000015', 'DEPOSITO', 1000.00, NULL, X'5eed0000000040008000000000000014',
       'Aporte inicial do idealizador', '2026-09-25 09:00:00'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_lancamento WHERE id = X'5eed0000000040008000000000000015')
  AND EXISTS (SELECT 1 FROM tb_conta WHERE id = X'5eed0000000040008000000000000014');

-- ---------- Renan Barros: conta de DESTINO ----------

INSERT INTO tb_endereco (id, cep, logradouro, bairro, cidade, uf, ibge, numero, complemento)
SELECT X'5eed0000000040008000000000000021', '06020-010', 'Avenida dos Autonomistas', 'Vila Yara', 'Osasco', 'SP', '3534401', '1000', 'Apto 12'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_endereco WHERE id = X'5eed0000000040008000000000000021')
  AND NOT EXISTS (SELECT 1 FROM tb_cliente WHERE cpf = '98765432100');

INSERT INTO tb_cliente (id, nome_completo, cpf, email, telefone, endereco_id)
SELECT X'5eed0000000040008000000000000022', 'Renan Barros', '98765432100', 'renan.barros@rkfbank.test', '11987650002',
       X'5eed0000000040008000000000000021'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente
                  WHERE id = X'5eed0000000040008000000000000022' OR cpf = '98765432100' OR email = 'renan.barros@rkfbank.test')
  AND EXISTS (SELECT 1 FROM tb_endereco WHERE id = X'5eed0000000040008000000000000021');

INSERT INTO tb_conta (id, numero_conta, agencia, tipo_conta_enum, saldo, ativa, versao, cliente_id)
SELECT X'5eed0000000040008000000000000024', '100002', '0001', 'CORRENTE', 500.00, TRUE, 0, X'5eed0000000040008000000000000022'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_conta WHERE id = X'5eed0000000040008000000000000024' OR numero_conta = '100002')
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = X'5eed0000000040008000000000000022');

INSERT INTO tb_lancamento (id, tipo_lancamento_enum, valor, conta_origem_id, conta_destino_id, descricao, data_hora)
SELECT X'5eed0000000040008000000000000025', 'DEPOSITO', 500.00, NULL, X'5eed0000000040008000000000000024',
       'Aporte inicial do idealizador', '2026-09-25 09:00:00'
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_lancamento WHERE id = X'5eed0000000040008000000000000025')
  AND EXISTS (SELECT 1 FROM tb_conta WHERE id = X'5eed0000000040008000000000000024');
