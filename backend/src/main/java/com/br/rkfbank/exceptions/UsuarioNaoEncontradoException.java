package com.br.rkfbank.exceptions;

// Gestao de usuarios (ADMIN): o usuario do id informado nao existe -> 404.
public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException() {
        super("Usuario nao encontrado");
    }
}
