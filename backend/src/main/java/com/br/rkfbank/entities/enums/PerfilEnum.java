package com.br.rkfbank.entities.enums;

public enum PerfilEnum {

    CLIENTE("Cliente do banco"),
    ADMIN("Administrador");

    private final String descricao;

    PerfilEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
