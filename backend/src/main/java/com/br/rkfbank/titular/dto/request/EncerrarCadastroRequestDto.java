package com.br.rkfbank.titular.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /clientes/me/encerramento: a senha confirma que e o proprio titular (um token esquecido aberto num
// computador nao basta para encerrar o cadastro de alguem).
public record EncerrarCadastroRequestDto(
        @NotBlank(message = "senha é obrigatória")
        @Size(max = 72, message = "senha deve ter no máximo 72 caracteres")
        String senha
) {

    @Override
    public String toString() {
        return "EncerrarCadastroRequestDto[senha=***]";
    }
}
