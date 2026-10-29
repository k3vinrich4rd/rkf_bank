package com.br.rkfbank.compartilhado.web;

import com.br.rkfbank.compartilhado.erros.RequisicaoInvalidaException;
import org.springframework.data.domain.PageRequest;

// Regras de paginacao compartilhadas pelos endpoints de listagem (padrao do Spring Data: page comeca em 0).
public final class Paginacao {

    // Teto para impedir que um unico request carregue a tabela inteira.
    public static final int MAX_PAGE_SIZE = 100;

    // Tamanho quando so o page vem na URL.
    public static final int TAMANHO_PADRAO = 10;

    private Paginacao() {
    }

    // Quer pagina? Basta ter page ou size na URL; sem nenhum dos dois, a rota devolve a lista inteira.
    public static boolean querPagina(Integer page, Integer size) {
        return page != null || size != null;
    }

    // O que faltar na URL usa o padrao: page 0, size 10.
    public static PageRequest criar(Integer page, Integer size) {
        int numero = page == null ? 0 : page;
        int tamanho = size == null ? TAMANHO_PADRAO : size;
        if (numero < 0) {
            throw new RequisicaoInvalidaException("page", "deve ser maior ou igual a zero");
        }
        if (tamanho < 1 || tamanho > MAX_PAGE_SIZE) {
            throw new RequisicaoInvalidaException("size", "deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(numero, tamanho);
    }
}
