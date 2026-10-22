package com.br.rkfbank.controllers;

import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import org.springframework.data.domain.PageRequest;

// Regras de paginacao compartilhadas pelos endpoints de listagem.
final class Paginacao {

    // Teto para impedir que um unico request carregue a tabela inteira.
    static final int MAX_PAGE_SIZE = 100;

    // Tamanho quando so o page vem na URL.
    static final int TAMANHO_PADRAO = 10;

    // Quer pagina? paginado (forma antiga) manda quando vier; senao, basta ter page ou size (padrao de mercado).
    static boolean querPagina(Boolean paginado, Integer page, Integer size) {
        return paginado != null ? paginado : page != null || size != null;
    }

    private Paginacao() {
    }

    static PageRequest criar(int page, int size) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("page", "deve ser maior ou igual a zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new RequisicaoInvalidaException("size", "deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size);
    }
}
