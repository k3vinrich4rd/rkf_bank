package com.br.rkfbank.dto.request.cliente;

import com.br.rkfbank.validation.CpfValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequestDto(
        @NotBlank(message = "nomeCompleto e obrigatório")
        @Size(max = 120, message = "nomeCompleto deve ter no máximo 120 caracteres")
        String nomeCompleto,
        @NotBlank(message = "cpf e obrigatório")
        @CpfValido(message = "cpf inválido")
        String cpf,
        @NotBlank(message = "email e obrigatório")
        @Email(message = "email invalido")
        String email,
        @NotBlank(message = "telefone e obrigatório")
        String telefone,
        @NotBlank(message = "cep e obrigatório")
        String cep,
        @NotBlank(message = "numero e obrigatório")
        String numero,
        @Size(max = 80, message = "complemento deve ter no máximo 80 caracteres")
        String complemento
) {
}