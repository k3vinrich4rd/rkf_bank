-- Idempotencia das movimentacoes (deposito, saque, transferencia).
-- O cliente manda o header Idempotency-Key (um UUID que ele gera por operacao). A primeira chamada com a chave
-- movimenta e grava uma linha aqui, NA MESMA TRANSACAO do dinheiro. Repetiu a chave (rede caiu, clique duplo,
-- retry automatico)? A API encontra a linha e responde 204 sem movimentar de novo.
-- hash_requisicao (SHA-256 em hexadecimal) prova que a repeticao e da MESMA operacao: chave igual com valor ou
-- conta diferentes e recusada (422), em vez de ser tratada como repeticao.
-- O UNIQUE (usuario_id, chave) e a trava final: duas chamadas simultaneas com a mesma chave nao gravam as duas.

CREATE TABLE tb_idempotencia (
    id              BINARY(16)   NOT NULL,
    usuario_id      BINARY(16)   NOT NULL,
    chave           VARCHAR(100) NOT NULL,
    operacao        VARCHAR(32)  NOT NULL,
    hash_requisicao VARCHAR(64)  NOT NULL,
    criado_em       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_idempotencia PRIMARY KEY (id),
    CONSTRAINT uk_idempotencia_usuario_chave UNIQUE (usuario_id, chave),
    CONSTRAINT fk_idempotencia_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id),
    CONSTRAINT ck_idempotencia_operacao CHECK (operacao IN ('DEPOSITO', 'SAQUE', 'TRANSFERENCIA'))
);
