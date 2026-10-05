package com.br.rkfbank.exceptions;

public class PaginacaoInvalidaException extends RuntimeException {

    public PaginacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
