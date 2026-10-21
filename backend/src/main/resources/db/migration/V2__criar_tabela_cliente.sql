-- Cliente do banco. CPF e telefone so com digitos; e-mail sempre em minusculas (o ClienteService normaliza),
-- assim o UNIQUE vale igual no MySQL (que ignora maiusculas) e no H2 (que nao ignora).

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
