package com.br.rkfbank.exceptions;

// Conflito com um dado que ja existe -> 409. Hoje e usada para duplicidade: CPF, e-mail ou numero de conta
// repetidos (inclusive quando a constraint UNIQUE do banco pega um cadastro simultaneo).
// Por que 409 e nao 422: o pedido em si esta certo, mas colide com o estado atual do banco (o recurso ja existe).
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) {
        super(mensagem);
    }
}