package com.br.rkfbank.exceptions;

// Entrada invalida que o Bean Validation nao consegue checar sozinho -> 400 com o campo em "campos".
// Ex.: page/size fora do limite, header Idempotency-Key ausente, senha com mais de 72 bytes (acentos).
public class RequisicaoInvalidaException extends RuntimeException {

    // Nome do campo como o cliente da API o enviou (mesmo nome do JSON ou do header), para o front marcar o input.
    private final String campo;

    public RequisicaoInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
