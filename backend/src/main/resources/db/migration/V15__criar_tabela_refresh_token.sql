-- Refresh token: renova a sessao sem pedir a senha de novo e da para ser REVOGADO (o JWT de 15 min nao da).
-- O login devolve um token de renovacao aleatorio; aqui fica so o SHA-256 dele (quem le o banco nao consegue usa-lo).
-- Cada renovacao revoga o usado e emite outro (rotacao). Usar um ja revogado = alguem copiou: todos os do usuario caem.
--
-- versao_credencial: a versao da senha no momento da emissao. Trocou ou redefiniu a senha -> a renovacao e recusada.
-- revogado_em:       NULL = ainda vale (ate expira_em). Preenchido no logout, na rotacao e na deteccao de reuso.

CREATE TABLE tb_refresh_token (
    id                BINARY(16)  NOT NULL,
    usuario_id        BINARY(16)  NOT NULL,
    hash_token        VARCHAR(64) NOT NULL,
    versao_credencial INT         NOT NULL,
    criado_em         DATETIME(6) NOT NULL,
    expira_em         DATETIME(6) NOT NULL,
    revogado_em       DATETIME(6),
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (hash_token),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id)
);

CREATE INDEX idx_refresh_token_usuario ON tb_refresh_token (usuario_id);
CREATE INDEX idx_refresh_token_expira_em ON tb_refresh_token (expira_em);
