-- Conta bancaria. Saldo em NUMERIC(19,2): dinheiro nunca em float/double.
-- ativa: BOOLEAN nativo do PostgreSQL (no MySQL era BIT).

CREATE TABLE tb_conta (
    id              UUID NOT NULL,
    numero_conta    VARCHAR(255)  NOT NULL,
    agencia         VARCHAR(4)    NOT NULL,
    tipo_conta_enum VARCHAR(20)   NOT NULL,
    saldo           NUMERIC(19,2) NOT NULL,
    ativa           BOOLEAN NOT NULL,
    -- Lock otimista (@Version): cada UPDATE confere e incrementa; duas escritas simultaneas -> 409.
    versao          BIGINT        NOT NULL,
    cliente_id      UUID NOT NULL,
    CONSTRAINT pk_conta PRIMARY KEY (id),
    CONSTRAINT uk_conta_numero UNIQUE (numero_conta),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente (id),
    CONSTRAINT ck_conta_tipo CHECK (tipo_conta_enum IN ('CORRENTE', 'POUPANCA'))
);
