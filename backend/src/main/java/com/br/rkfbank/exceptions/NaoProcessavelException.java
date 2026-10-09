package com.br.rkfbank.exceptions;

public class NaoProcessavelException extends RuntimeException {
    public NaoProcessavelException(String mensagem) {
        super(mensagem);
    }
}