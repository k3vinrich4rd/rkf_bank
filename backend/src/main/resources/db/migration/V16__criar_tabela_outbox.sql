-- Transactional Outbox (comprovante assincrono das movimentacoes).
-- Cada deposito, saque e transferencia grava aqui um evento NA MESMA TRANSACAO do dinheiro: deu rollback, o evento
-- some junto; deu commit, o evento esta garantido. O PublicadorOutbox (a cada 2 s, uma instancia so pelo ShedLock)
-- entrega os pendentes ao notificador de comprovantes e marca publicado_em.
--
-- payload: o evento em JSON, sem dado pessoal (ids, tipo, valor e data; nada de nome, CPF ou descricao livre).
-- tentativas / ultimo_erro: falhou a entrega, conta mais uma; com 5, o evento para de ser tentado e fica como uma
-- "fila de mortos" (DLQ) para o ADMIN olhar no banco.
CREATE TABLE tb_outbox (
    id           BINARY(16)    NOT NULL,
    tipo         VARCHAR(40)   NOT NULL,
    payload      VARCHAR(1000) NOT NULL,
    criado_em    DATETIME(6)   NOT NULL,
    publicado_em DATETIME(6),
    tentativas   INT           NOT NULL DEFAULT 0,
    ultimo_erro  VARCHAR(255),
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);

-- O publicador busca "nao publicados, do mais antigo"; a limpeza diaria apaga os publicados ha mais de 30 dias.
CREATE INDEX idx_outbox_publicado_criado ON tb_outbox (publicado_em, criado_em);
