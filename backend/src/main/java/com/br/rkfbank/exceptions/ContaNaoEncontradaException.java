package com.br.rkfbank.exceptions;

// A conta do id informado nao existe -> 404. Na rota de quem nao e dono, o 403 vem antes (nao revela se a conta existe).
public class ContaNaoEncontradaException extends RuntimeException {
    public ContaNaoEncontradaException() {
        super("Conta nao encontrada");
    }
}