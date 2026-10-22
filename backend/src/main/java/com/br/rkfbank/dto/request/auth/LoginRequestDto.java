package com.br.rkfbank.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo do POST /auth/login. O CPF pode vir com ou sem pontuacao (o AutenticacaoService tira tudo que nao e digito).
// Limite de 72 na senha: o BCrypt ignora o que passa de 72 bytes, entao aceitar mais daria falsa seguranca.
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
