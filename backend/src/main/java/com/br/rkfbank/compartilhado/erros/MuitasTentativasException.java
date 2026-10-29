package com.br.rkfbank.compartilhado.erros;

// 429: o mesmo CPF errou a senha vezes demais em pouco tempo. Leva quantos segundos faltam para liberar (Retry-After).
public class MuitasTentativasException extends ErroApiException {

    private final long segundosParaLiberar;

    public MuitasTentativasException(long segundosParaLiberar) {
        super(CodigoErroEnum.MUITAS_TENTATIVAS);
        this.segundosParaLiberar = segundosParaLiberar;
    }

    public long getSegundosParaLiberar() {
        return segundosParaLiberar;
    }
}
