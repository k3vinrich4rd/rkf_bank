package com.br.rkfbank.dto.request.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequestDto(
    @NotBlank(message = "nomeCompleto e obrigatório")
    @Size(max = 120, message = "nomeCompleto deve ter no máximo 120 caracteres")
    String nomeCompleto,
    @NotBlank(message = "cpf e obrigatório")
    @Pattern(regexp = "\\d{11}", message = "cpf deve ter 11 dígitos numéricos")
    String cpf,
    @NotBlank(message = "email e obrigatório")
    @Email(message = "email invalido")
    String email,
    @NotBlank(message = "telefone e obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 dígitos")
    String telefone,
    @NotBlank(message = "cep e obrigatório")
    @Pattern(regexp = "\\d{8}", message = "cep deve ter 8 dígitos numéricos")
    String cep,
    @NotBlank(message = "numero e obrigatório")
    String numero,
    @Size(max = 80, message = "complemento deve ter no máximo 80 caracteres")
    String complemento
) {}