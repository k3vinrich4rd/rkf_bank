package com.br.rkfbank.compartilhado.web;

// Um erro de validacao: qual campo (mesmo nome do JSON) e o que esta errado nele.
public record ErroCampoResponse(
    String campo,
    String mensagem
) {}

