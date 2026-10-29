package com.br.rkfbank.compartilhado.erros;

// Base das excecoes que viram resposta de erro: todas carregam um CodigoErroEnum, e a mensagem vem dele.
// Cada subclasse continua existindo porque e ela que decide o STATUS no TratadorGlobalDeErros
// (RegraNegocio -> 422, Conflito -> 409...). RuntimeException (unchecked) de proposito: o @Transactional so
// desfaz a transacao sozinho para unchecked, e o service nao precisa declarar "throws" em cada metodo.
public abstract class ErroApiException extends RuntimeException {

    private final CodigoErroEnum codigo;

    protected ErroApiException(CodigoErroEnum codigo) {
        super(codigo.getMensagem());
        this.codigo = codigo;
    }

    // Guarda a causa tecnica (timeout, erro de rede) para o log; ela nunca vai para a resposta.
    protected ErroApiException(CodigoErroEnum codigo, Throwable causa) {
        super(codigo.getMensagem(), causa);
        this.codigo = codigo;
    }

    public CodigoErroEnum getCodigo() {
        return codigo;
    }
}
