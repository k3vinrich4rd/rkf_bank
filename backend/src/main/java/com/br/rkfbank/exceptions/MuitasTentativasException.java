package com.br.rkfbank.exceptions;

// 429: o mesmo CPF errou a senha vezes demais em pouco tempo. Leva quantos segundos faltam para liberar (Retry-After).
public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaLiberar;

    public MuitasTentativasException(long segundosParaLiberar) {
        super("Muitas tentativas de login; aguarde alguns minutos e tente de novo");
        this.segundosParaLiberar = segundosParaLiberar;
    }

    public long getSegundosParaLiberar() {
        return segundosParaLiberar;
    }
}
