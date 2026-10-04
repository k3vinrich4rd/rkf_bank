package com.br.rkfbank.dto.response.cliente;

import java.util.UUID;

public record ClienteResponseDto(
    UUID id,
    String nomeCompleto,
    String cpf,
    String email,
    String telefone,
    EnderecoResponseDto enderecoResponseDto
) {}