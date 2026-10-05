package com.br.rkfbank.exceptions;

public class ServicoExternoException extends RuntimeException {
    public ServicoExternoException(String mensagem) {
        super(mensagem);
    }
}