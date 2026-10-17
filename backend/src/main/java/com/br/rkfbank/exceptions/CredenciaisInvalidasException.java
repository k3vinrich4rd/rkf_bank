package com.br.rkfbank.exceptions;

// Mesma mensagem para CPF inexistente e senha errada: nao contar a um atacante quais CPFs tem cadastro.
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("CPF ou senha invalidos");
    }
}
