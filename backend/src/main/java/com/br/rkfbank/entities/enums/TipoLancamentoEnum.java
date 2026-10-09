package com.br.rkfbank.entities.enums;

public enum TipoLancamentoEnum {

    DEPOSITO("Depósito"),
    SAQUE("Saque"),
    TRANSFERENCIA("Transferência"),
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
