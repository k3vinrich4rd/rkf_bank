package com.br.rkfbank.entities.enums;

public enum TipoLancamentoEnum {

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
