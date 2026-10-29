package com.br.rkfbank.compartilhado.erros;

// O cliente pedido nao existe -> 404 no TratadorGlobalDeErros.
public class ClienteNaoEncontradoException extends ErroApiException {
    public ClienteNaoEncontradoException() {
        super(CodigoErroEnum.CLIENTE_NAO_ENCONTRADO);
    }
}
