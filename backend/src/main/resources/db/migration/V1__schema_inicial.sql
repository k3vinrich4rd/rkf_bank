-- Schema inicial do RKF Bank: o mesmo que o Hibernate gerava com ddl-auto=update, agora versionado.
-- Regra: migracao aplicada nunca se edita. Mudou algo? Crie V2__, V3__...
-- Tipos seguem o que o Hibernate espera para validar as entidades (ddl-auto=validate):
-- UUID -> BINARY(16), BigDecimal -> DECIMAL(19,2), boolean -> BIT, Instant -> DATETIME(6).
-- BIT sem tamanho: no MySQL vale BIT(1); o H2 dos testes nao aceita "BIT(1)".

CREATE TABLE tb_endereco (
    id          BINARY(16)   NOT NULL,
    cep         VARCHAR(9)   NOT NULL,
    logradouro  VARCHAR(255) NOT NULL,
    bairro      VARCHAR(255) NOT NULL,
    cidade      VARCHAR(255) NOT NULL,
    uf          VARCHAR(2)   NOT NULL,
    ibge        VARCHAR(255) NOT NULL,
    numero      VARCHAR(255) NOT NULL,
    complemento VARCHAR(255),
    CONSTRAINT pk_endereco PRIMARY KEY (id)
);

CREATE TABLE tb_cliente (
    id            BINARY(16)   NOT NULL,
    nome_completo VARCHAR(255) NOT NULL,
    cpf           VARCHAR(11)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    telefone      VARCHAR(255) NOT NULL,
    endereco_id   BINARY(16)   NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_cpf UNIQUE (cpf),
    CONSTRAINT uk_cliente_email UNIQUE (email),
    -- @OneToOne: cada endereco pertence a um cliente so.
    CONSTRAINT uk_cliente_endereco UNIQUE (endereco_id),
    CONSTRAINT fk_cliente_endereco FOREIGN KEY (endereco_id) REFERENCES tb_endereco (id)
);

CREATE TABLE tb_conta (
    id              BINARY(16)    NOT NULL,
    numero_conta    VARCHAR(255)  NOT NULL,
    agencia         VARCHAR(4)    NOT NULL,
    tipo_conta_enum VARCHAR(20)   NOT NULL,
    saldo           DECIMAL(19,2) NOT NULL,
    ativa           BIT           NOT NULL,
    -- Lock otimista (@Version): cada UPDATE confere e incrementa.
    versao          BIGINT        NOT NULL,
    cliente_id      BINARY(16)    NOT NULL,
    CONSTRAINT pk_conta PRIMARY KEY (id),
    CONSTRAINT uk_conta_numero UNIQUE (numero_conta),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente (id),
    CONSTRAINT ck_conta_tipo CHECK (tipo_conta_enum IN ('CORRENTE', 'POUPANCA'))
);

CREATE TABLE tb_lancamento (
    id                   BINARY(16)    NOT NULL,
    tipo_lancamento_enum VARCHAR(32)   NOT NULL,
    valor                DECIMAL(19,2) NOT NULL,
    -- Sem FK de proposito: deposito nao tem origem e saque nao tem destino.
    conta_origem_id      BINARY(16),
    conta_destino_id     BINARY(16),
    descricao            VARCHAR(255),
    data_hora            DATETIME(6)   NOT NULL,
    CONSTRAINT pk_lancamento PRIMARY KEY (id),
    CONSTRAINT ck_lancamento_tipo CHECK (tipo_lancamento_enum IN ('DEPOSITO', 'SAQUE', 'TRANSFERENCIA', 'CREDITO', 'DEBITO'))
);

-- O extrato busca por origem OU destino; sem indice, cada consulta varre a tabela inteira.
CREATE INDEX idx_lancamento_origem ON tb_lancamento (conta_origem_id, data_hora);
CREATE INDEX idx_lancamento_destino ON tb_lancamento (conta_destino_id, data_hora);
