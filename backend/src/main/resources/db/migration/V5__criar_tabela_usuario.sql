-- Credenciais de acesso (login com CPF + senha), separadas do cadastro do cliente:
-- dado cadastral e segredo nao andam juntos, e o ADMIN pode existir sem ser cliente.
-- senha_hash guarda o BCrypt (sempre 60 caracteres, com o salt dentro); a senha em texto nunca chega ao banco.
-- cliente_id e opcional (nulo para o ADMIN). O UNIQUE garante no maximo um usuario por cliente.

CREATE TABLE tb_usuario (
    id          BINARY(16)  NOT NULL,
    login       VARCHAR(11) NOT NULL,
    senha_hash  VARCHAR(60) NOT NULL,
    perfil      VARCHAR(20) NOT NULL,
    cliente_id  BINARY(16),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT uk_usuario_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES tb_cliente (id),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('CLIENTE', 'ADMIN'))
);
