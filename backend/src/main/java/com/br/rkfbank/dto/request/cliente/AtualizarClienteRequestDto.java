package com.br.rkfbank.dto.request.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PATCH /clientes/me (LGPD, direito de correcao). Todos opcionais: so muda o que vier preenchido.
// CPF fica de fora de proposito: e o login e a chave do cadastro; CPF errado se corrige pelo atendimento.
// Mudou o CEP? O endereco e buscado de novo no ViaCEP, e o numero passa a ser obrigatorio (casa nova, numero novo).
public record AtualizarClienteRequestDto(
        @Size(min = 1, max = 120, message = "nomeCompleto deve ter entre 1 e 120 caracteres")
        String nomeCompleto,
        @Email(message = "email invalido")
        @Size(max = 255, message = "email deve ter no maximo 255 caracteres")
        String email,
        @Pattern(regexp = "[\\d()\\s+-]{10,20}", message = "telefone invalido")
        String telefone,
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "cep deve ter 8 digitos")
        String cep,
        @Size(min = 1, max = 20, message = "numero deve ter entre 1 e 20 caracteres")
        String numero,
        // "" apaga o complemento; null deixa como esta.
        @Size(max = 80, message = "complemento deve ter no maximo 80 caracteres")
        String complemento
) {

    public boolean vazio() {
        return nomeCompleto == null && email == null && telefone == null && cep == null && numero == null && complemento == null;
    }

    // O toString padrao do record imprimiria e-mail, telefone e endereco em qualquer log do DTO.
    @Override
    public String toString() {
        return "AtualizarClienteRequestDto[***]";
    }
}
