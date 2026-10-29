package com.br.rkfbank.titular.dto.response;

// GET /termos: a versao dos Termos de Uso e da Politica de Privacidade que o cadastro aceita hoje.
public record TermosResponseDto(
    String versao
) {}
