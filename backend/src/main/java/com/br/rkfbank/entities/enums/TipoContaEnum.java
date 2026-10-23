package com.br.rkfbank.entities.enums;

// Tipos de conta que o banco oferece. Gravado como texto (@Enumerated(STRING)): se gravasse a posicao (0, 1),
// colocar um tipo novo no meio da lista trocaria o significado das contas que ja existem.
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
