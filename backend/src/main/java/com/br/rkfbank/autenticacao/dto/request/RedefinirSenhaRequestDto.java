package com.br.rkfbank.autenticacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Redefinicao pelo ADMIN (PATCH /admin/usuarios/{id}/senha): para quem esqueceu a senha. O admin passa a senha nova
// para a pessoa por canal privado, e ela troca pela propria no primeiro acesso (PATCH /usuarios/me/senha).
public record RedefinirSenhaRequestDto(
        @NotBlank(message = "novaSenha é obrigatória")
        @Size(min = 8, max = 72, message = "novaSenha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {

    // Record gera um toString com todos os campos. Sobrescrito para a senha nunca aparecer num log ou erro.
    @Override
    public String toString() {
        return "RedefinirSenhaRequestDto[novaSenha=***]";
    }
}
