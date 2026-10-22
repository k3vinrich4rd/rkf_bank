-- LGPD: o titular pode pedir o encerramento do cadastro (POST /clientes/me/encerramento).
-- Banco nao apaga cliente na hora: a lei de prevencao a lavagem de dinheiro (Lei 9.613/1998, art. 10) manda guardar
-- os registros por 5 anos depois do fim do relacionamento. Por isso o encerramento so marca a data, e uma rotina
-- diaria anonimiza o cadastro quando o prazo vence (troca nome, CPF, e-mail, telefone e endereco por valores
-- que nao identificam ninguem). Os lancamentos ficam: sao registro financeiro e nao guardam dado pessoal direto.
--
-- encerrado_em:   quando o titular encerrou (NULL = cliente ativo).
-- anonimizado_em: quando a rotina anonimizou (NULL = dados ainda guardados).
-- Instant -> DATETIME(6), como nas outras tabelas. Colunas novas e opcionais: nao mexe em nenhum cliente existente.

ALTER TABLE tb_cliente ADD COLUMN encerrado_em DATETIME(6);
ALTER TABLE tb_cliente ADD COLUMN anonimizado_em DATETIME(6);

-- A rotina procura "encerrados ha mais de 5 anos e ainda nao anonimizados" todo dia.
CREATE INDEX idx_cliente_encerrado_em ON tb_cliente (encerrado_em);
