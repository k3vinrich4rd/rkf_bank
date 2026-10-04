package com.br.rkfbank.entities.enums;

public enum TipoContaEnum {

    CORRENTE("Conta Corrente"),
    POUPANCA("Conta Poupança");

    private final String descricao;

    TipoContaEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
