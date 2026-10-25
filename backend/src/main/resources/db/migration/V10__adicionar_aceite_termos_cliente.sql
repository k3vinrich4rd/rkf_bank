-- LGPD: o cadastro registra QUAL versao dos Termos de Uso e da Politica de Privacidade o titular aceitou e QUANDO.
-- Sem esse registro, o "voce concorda com os termos" da tela de cadastro nao prova nada (o controlador precisa
-- demonstrar o consentimento e a base legal, LGPD art. 8 e 37).
--
-- termos_versao:     a versao aceita (ex.: 2026-10-23); muda quando o texto dos termos muda.
-- termos_aceitos_em: o instante do aceite. Instant -> DATETIME(6), como nas outras tabelas.
-- Colunas opcionais: clientes criados antes desta versao ficam com NULL (aceite ainda nao registrado).

ALTER TABLE tb_cliente ADD COLUMN termos_versao VARCHAR(20);
ALTER TABLE tb_cliente ADD COLUMN termos_aceitos_em DATETIME(6);
