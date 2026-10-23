package com.br.rkfbank.entities.enums;

// Tipo de cada linha do extrato. O front decide entrada x saida pelo contaDestinoId, nao pelo tipo.
public enum TipoLancamentoEnum {

    DEPOSITO("Depósito"),
    SAQUE("Saque"),
    TRANSFERENCIA("Transferência"),
    // CREDITO e DEBITO sao tipos genericos reservados para lancamentos que nao sao deposito, saque nem transferencia
    // (tarifa, estorno, rendimento). Nenhuma operacao os gera hoje; ficam no enum para o CHECK do banco ja aceita-los.
    CREDITO("Crédito"),
    DEBITO("Débito");

    private final String descricao;

    TipoLancamentoEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
