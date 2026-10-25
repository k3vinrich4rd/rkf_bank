-- CREDITO e DEBITO estavam reservados desde a V4 e nenhuma operacao os gerava: sairam do enum TipoLancamentoEnum.
-- O CHECK da coluna acompanha, para o banco recusar um tipo que o codigo nao conhece.
-- (Nenhum lancamento tem esses tipos: nenhuma rota os criou. Se um banco local tiver, a migracao falha e avisa.)
-- DROP CONSTRAINT funciona no MySQL 8.0.19+ e no H2.

ALTER TABLE tb_lancamento DROP CONSTRAINT ck_lancamento_tipo;
ALTER TABLE tb_lancamento ADD CONSTRAINT ck_lancamento_tipo
    CHECK (tipo_lancamento_enum IN ('DEPOSITO', 'SAQUE', 'TRANSFERENCIA'));
