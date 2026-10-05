package com.br.rkfbank.dto.comum;

public record ErroCampoResponse(
    String campo,
    String mensagem
) {}

