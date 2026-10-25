package com.br.rkfbank.exceptions;

// Usuario autenticado tentando usar a conta de outro cliente (regra de dono) -> 403.
public class AcessoNegadoException extends ErroApiException {
    public AcessoNegadoException() {
        super(CodigoErroEnum.CONTA_DE_OUTRO_CLIENTE);
    }
}
