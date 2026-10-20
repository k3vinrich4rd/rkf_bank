package com.br.rkfbank.dto.request.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Redefinicao pelo ADMIN (PATCH /admin/usuarios/{id}/senha): para quem esqueceu a senha. O admin passa a senha nova
// para a pessoa por canal privado, e ela troca pela propria no primeiro acesso (PATCH /auth/senha).
public record RedefinirSenhaRequestDto(
        @NotBlank(message = "novaSenha e obrigatória")
        @Size(min = 8, max = 72, message = "novaSenha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {

    @Override
    public String toString() {
        return "RedefinirSenhaRequestDto[novaSenha=***]";
    }
}
