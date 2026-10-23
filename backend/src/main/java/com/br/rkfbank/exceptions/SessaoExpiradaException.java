package com.br.rkfbank.exceptions;

// O token de renovacao nao serve mais (vencido, revogado, desconhecido, usuario desativado ou senha trocada) -> 401.
// Mesma resposta para todos os casos: quem tenta adivinhar token nao descobre qual foi.
public class SessaoExpiradaException extends ErroApiException {
    public SessaoExpiradaException() {
        super(CodigoErroEnum.SESSAO_EXPIRADA);
    }
}
