package com.br.rkfbank.exceptions;

public class RequisicaoInvalidaException extends RuntimeException {

    private final String campo;

    public RequisicaoInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
