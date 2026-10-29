package com.br.rkfbank.compartilhado.erros;

// Conflito com o estado atual do banco -> 409: CPF, e-mail ou numero de conta repetidos (inclusive quando a
// constraint UNIQUE pega um cadastro simultaneo) e a mesma Idempotency-Key em duas chamadas ao mesmo tempo.
// Por que 409 e nao 422: o pedido em si esta certo, mas colide com o que ja existe.
public class ConflitoException extends ErroApiException {
    public ConflitoException(CodigoErroEnum codigo) {
        super(codigo);
    }
}
