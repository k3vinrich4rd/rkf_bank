package com.br.rkfbank.dto.response.cliente;

// GET /termos: a versao dos Termos de Uso e da Politica de Privacidade que o cadastro aceita hoje.
public record TermosResponseDto(
    String versao
) {}
