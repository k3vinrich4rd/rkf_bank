package com.br.rkfbank.dto.response.cliente;

import java.util.UUID;

// Cadastro devolvido pela API: CPF e telefone ja formatados para exibir. A senha (e o hash) nunca entram aqui.
public record ClienteResponseDto(
    UUID id,
    String nomeCompleto,
    String cpf,
    String email,
    String telefone,
    EnderecoResponseDto endereco
) {}