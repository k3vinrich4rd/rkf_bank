package com.br.rkfbank.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Troca da propria senha (PATCH /usuarios/me/senha). A senha atual prova que e o dono, e nao alguem com um token roubado.
public record TrocarSenhaRequestDto(
        @NotBlank(message = "senhaAtual é obrigatória")
        @Size(max = 72, message = "senhaAtual deve ter no máximo 72 caracteres")
        String senhaAtual,
        @NotBlank(message = "novaSenha é obrigatória")
        @Size(min = 8, max = 72, message = "novaSenha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {

    // O toString padrao do record imprimiria as duas senhas em qualquer log do DTO.
    @Override
    public String toString() {
        return "TrocarSenhaRequestDto[senhaAtual=***, novaSenha=***]";
    }
}
