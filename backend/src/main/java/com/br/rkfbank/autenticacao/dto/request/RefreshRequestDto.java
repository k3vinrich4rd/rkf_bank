package com.br.rkfbank.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /auth/refresh e POST /auth/logout: o token de renovacao que o login (ou a renovacao anterior) devolveu.
public record RefreshRequestDto(
        @NotBlank(message = "refreshToken é obrigatório")
        @Size(max = 200, message = "refreshToken deve ter no máximo 200 caracteres")
        String refreshToken
) {

    // Com o token, qualquer um renova a sessao: o toString nunca o mostra.
    @Override
    public String toString() {
        return "RefreshRequestDto[refreshToken=***]";
    }
}
