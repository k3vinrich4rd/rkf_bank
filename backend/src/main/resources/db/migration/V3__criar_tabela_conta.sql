-- Conta bancaria. Saldo em DECIMAL(19,2): dinheiro nunca em float/double.
-- BIT sem tamanho: no MySQL vale BIT(1); o H2 dos testes nao aceita "BIT(1)".

CREATE TABLE tb_conta (
    id              BINARY(16)    NOT NULL,
    numero_conta    VARCHAR(255)  NOT NULL,
    agencia         VARCHAR(4)    NOT NULL,
    tipo_conta_enum VARCHAR(20)   NOT NULL,
    saldo           DECIMAL(19,2) NOT NULL,
    ativa           BIT           NOT NULL,
    -- Lock otimista (@Version): cada UPDATE confere e incrementa; duas escritas simultaneas -> 409.
    versao          BIGINT        NOT NULL,
    cliente_id      BINARY(16)    NOT NULL,
    CONSTRAINT pk_conta PRIMARY KEY (id),
    CONSTRAINT uk_conta_numero UNIQUE (numero_conta),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente (id),
    CONSTRAINT ck_conta_tipo CHECK (tipo_conta_enum IN ('CORRENTE', 'POUPANCA'))
);
