-- Lancamentos do extrato. Nunca sao alterados nem apagados: o extrato e o historico do dinheiro.

CREATE TABLE tb_lancamento (
    id                   UUID NOT NULL,
    tipo_lancamento_enum VARCHAR(32)   NOT NULL,
    valor                NUMERIC(19,2) NOT NULL,
    -- Sem FK de proposito: deposito nao tem origem e saque nao tem destino.
    conta_origem_id      UUID,
    conta_destino_id     UUID,
    descricao            VARCHAR(255),
    data_hora            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_lancamento PRIMARY KEY (id),
    CONSTRAINT ck_lancamento_tipo CHECK (tipo_lancamento_enum IN ('DEPOSITO', 'SAQUE', 'TRANSFERENCIA', 'CREDITO', 'DEBITO'))
);

-- O extrato busca por origem OU destino, do mais novo ao mais antigo; sem indice, cada consulta varre a tabela.
CREATE INDEX idx_lancamento_origem ON tb_lancamento (conta_origem_id, data_hora);
CREATE INDEX idx_lancamento_destino ON tb_lancamento (conta_destino_id, data_hora);
