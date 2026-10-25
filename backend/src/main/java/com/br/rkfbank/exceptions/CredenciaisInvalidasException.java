package com.br.rkfbank.exceptions;

// Mesma mensagem para CPF inexistente e senha errada: nao contar a um atacante quais CPFs tem cadastro.
public class CredenciaisInvalidasException extends ErroApiException {
    public CredenciaisInvalidasException() {
        super(CodigoErroEnum.CREDENCIAIS_INVALIDAS);
    }
}
