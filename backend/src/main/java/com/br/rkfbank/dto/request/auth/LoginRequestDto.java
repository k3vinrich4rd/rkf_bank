package com.br.rkfbank.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "cpf e obrigatório")
        String cpf,
        @NotBlank(message = "senha e obrigatória")
        @Size(max = 72, message = "senha deve ter no máximo 72 caracteres")
        String senha
) {

    // O toString padrao do record imprimiria CPF e senha em qualquer log do DTO.
    @Override
    public String toString() {
        return "LoginRequestDto[cpf=***, senha=***]";
    }
}
