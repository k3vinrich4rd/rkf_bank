package com.br.rkfbank.exceptions;

// O cliente pedido nao existe -> 404 no ControllerExceptionHandler.
// RuntimeException (unchecked) de proposito: o @Transactional so desfaz a transacao sozinho para unchecked,
// e o service nao precisa declarar "throws" em cada metodo.
public class ClienteNaoEncontradoException extends RuntimeException {
    public ClienteNaoEncontradoException() {
        super("Cliente não encontrado");
    }
}