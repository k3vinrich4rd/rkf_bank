-- Contas de VISITANTE: so no perfil visitante (H2 em memoria), para quem chega pelo GitHub testar a API.
--
--   Ana Visitante (Origem)    -> conta ORIGEM  0001 / 900001, saldo inicial R$ 1.000,00
--   Bruno Visitante (Destino) -> conta DESTINO 0001 / 900002, saldo inicial R$ 500,00
--   Login dos dois: CPF + senha Visitante@2026 (PUBLICA, esta no VISITANTES.md; o banco guarda so o hash BCrypt).
--
-- Senha publica aqui e aceitavel porque: (1) o perfil visitante so roda na maquina de quem testa, com H2 em memoria
-- que some ao parar; (2) o segredo do JWT desse perfil e sorteado a cada subida (rkfbank.jwt.segredo-aleatorio),
-- entao ninguem forja token; (3) nenhuma conta da equipe nem admin existe nesse perfil.
-- Mesmo esquema do seed da equipe: callback afterMigrate, idempotente, ids fixos ("a0000000..." = visitante).
-- CPFs ficticios, validos pelos digitos verificadores. Nomes e enderecos ficticios.

-- ---------- Ana Visitante: conta de ORIGEM ----------

INSERT INTO tb_endereco (id, cep, logradouro, bairro, cidade, uf, ibge, numero, complemento)
SELECT CAST('a0000000-0000-4000-8000-000000000111' AS UUID), '01001-000', 'Praça da Sé', 'Sé', 'São Paulo', 'SP', '3550308', '100', NULL
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_endereco WHERE id = CAST('a0000000-0000-4000-8000-000000000111' AS UUID))
  AND NOT EXISTS (SELECT 1 FROM tb_cliente WHERE cpf = '24681357928');

INSERT INTO tb_cliente (id, nome_completo, cpf, email, telefone, endereco_id)
SELECT CAST('a0000000-0000-4000-8000-000000000112' AS UUID), 'Ana Visitante (Origem)', '24681357928', 'ana.visitante@exemplo.test', '11900000001',
       CAST('a0000000-0000-4000-8000-000000000111' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente
                  WHERE id = CAST('a0000000-0000-4000-8000-000000000112' AS UUID) OR cpf = '24681357928' OR email = 'ana.visitante@exemplo.test')
  AND EXISTS (SELECT 1 FROM tb_endereco WHERE id = CAST('a0000000-0000-4000-8000-000000000111' AS UUID));

INSERT INTO tb_usuario (id, login, senha_hash, perfil, cliente_id)
SELECT CAST('a0000000-0000-4000-8000-000000000113' AS UUID), '24681357928',
       '$2a$10$sxBEceldX7ASvK.v0uzei.7kR8704Py5iV5tc7TSUnFYRbd6FgzBW', 'CLIENTE', CAST('a0000000-0000-4000-8000-000000000112' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_usuario
                  WHERE id = CAST('a0000000-0000-4000-8000-000000000113' AS UUID) OR login = '24681357928'
                     OR cliente_id = CAST('a0000000-0000-4000-8000-000000000112' AS UUID))
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = CAST('a0000000-0000-4000-8000-000000000112' AS UUID));

INSERT INTO tb_conta (id, numero_conta, agencia, tipo_conta_enum, saldo, ativa, versao, cliente_id)
SELECT CAST('a0000000-0000-4000-8000-000000000114' AS UUID), '900001', '0001', 'CORRENTE', 1000.00, TRUE, 0, CAST('a0000000-0000-4000-8000-000000000112' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_conta WHERE id = CAST('a0000000-0000-4000-8000-000000000114' AS UUID) OR numero_conta = '900001')
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = CAST('a0000000-0000-4000-8000-000000000112' AS UUID));

INSERT INTO tb_lancamento (id, tipo_lancamento_enum, valor, conta_origem_id, conta_destino_id, descricao, data_hora)
SELECT CAST('a0000000-0000-4000-8000-000000000115' AS UUID), 'DEPOSITO', 1000.00, NULL, CAST('a0000000-0000-4000-8000-000000000114' AS UUID),
       'Saldo de boas-vindas do visitante', CAST('2026-09-25 09:00:00+00' AS TIMESTAMP WITH TIME ZONE)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_lancamento WHERE id = CAST('a0000000-0000-4000-8000-000000000115' AS UUID))
  AND EXISTS (SELECT 1 FROM tb_conta WHERE id = CAST('a0000000-0000-4000-8000-000000000114' AS UUID));

-- ---------- Bruno Visitante: conta de DESTINO ----------

INSERT INTO tb_endereco (id, cep, logradouro, bairro, cidade, uf, ibge, numero, complemento)
SELECT CAST('a0000000-0000-4000-8000-000000000121' AS UUID), '30140-071', 'Rua dos Aimorés', 'Boa Viagem', 'Belo Horizonte', 'MG', '3106200', '200', NULL
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_endereco WHERE id = CAST('a0000000-0000-4000-8000-000000000121' AS UUID))
  AND NOT EXISTS (SELECT 1 FROM tb_cliente WHERE cpf = '97531864282');

INSERT INTO tb_cliente (id, nome_completo, cpf, email, telefone, endereco_id)
SELECT CAST('a0000000-0000-4000-8000-000000000122' AS UUID), 'Bruno Visitante (Destino)', '97531864282', 'bruno.visitante@exemplo.test', '31900000002',
       CAST('a0000000-0000-4000-8000-000000000121' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_cliente
                  WHERE id = CAST('a0000000-0000-4000-8000-000000000122' AS UUID) OR cpf = '97531864282' OR email = 'bruno.visitante@exemplo.test')
  AND EXISTS (SELECT 1 FROM tb_endereco WHERE id = CAST('a0000000-0000-4000-8000-000000000121' AS UUID));

INSERT INTO tb_usuario (id, login, senha_hash, perfil, cliente_id)
SELECT CAST('a0000000-0000-4000-8000-000000000123' AS UUID), '97531864282',
       '$2a$10$sxBEceldX7ASvK.v0uzei.7kR8704Py5iV5tc7TSUnFYRbd6FgzBW', 'CLIENTE', CAST('a0000000-0000-4000-8000-000000000122' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_usuario
                  WHERE id = CAST('a0000000-0000-4000-8000-000000000123' AS UUID) OR login = '97531864282'
                     OR cliente_id = CAST('a0000000-0000-4000-8000-000000000122' AS UUID))
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = CAST('a0000000-0000-4000-8000-000000000122' AS UUID));

INSERT INTO tb_conta (id, numero_conta, agencia, tipo_conta_enum, saldo, ativa, versao, cliente_id)
SELECT CAST('a0000000-0000-4000-8000-000000000124' AS UUID), '900002', '0001', 'CORRENTE', 500.00, TRUE, 0, CAST('a0000000-0000-4000-8000-000000000122' AS UUID)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_conta WHERE id = CAST('a0000000-0000-4000-8000-000000000124' AS UUID) OR numero_conta = '900002')
  AND EXISTS (SELECT 1 FROM tb_cliente WHERE id = CAST('a0000000-0000-4000-8000-000000000122' AS UUID));

INSERT INTO tb_lancamento (id, tipo_lancamento_enum, valor, conta_origem_id, conta_destino_id, descricao, data_hora)
SELECT CAST('a0000000-0000-4000-8000-000000000125' AS UUID), 'DEPOSITO', 500.00, NULL, CAST('a0000000-0000-4000-8000-000000000124' AS UUID),
       'Saldo de boas-vindas do visitante', CAST('2026-09-25 09:00:00+00' AS TIMESTAMP WITH TIME ZONE)
FROM (SELECT 1 AS um) unica
WHERE NOT EXISTS (SELECT 1 FROM tb_lancamento WHERE id = CAST('a0000000-0000-4000-8000-000000000125' AS UUID))
  AND EXISTS (SELECT 1 FROM tb_conta WHERE id = CAST('a0000000-0000-4000-8000-000000000124' AS UUID));

-- Bancos H2 do perfil visitante somem ao parar a API, entao nao precisam de UPDATE: cada subida ja nasce com o nome novo.
