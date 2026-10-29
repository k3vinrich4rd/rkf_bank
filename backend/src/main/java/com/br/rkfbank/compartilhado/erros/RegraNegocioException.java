package com.br.rkfbank.compartilhado.erros;

// Regra de negocio recusou a operacao -> 422 (Unprocessable Content): o pedido esta bem formado, mas nao pode
// ser cumprido. Ex.: saldo insuficiente, conta inativa, transferir para a mesma conta, CEP inexistente.
// Diferente do 400 (formato errado) e do 409 (conflito com o que ja existe).
public class RegraNegocioException extends ErroApiException {
    public RegraNegocioException(CodigoErroEnum codigo) {
        super(codigo);
    }
}
