package com.br.rkfbank.exceptions;

// Usuario autenticado tentando usar recurso de outro cliente (regra de dono) -> 403.
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
