package com.br.rkfbank.dto.response.cliente;

public record EnderecoResponseDto(
    String cep,
    String logradouro,
    String bairro,
    String cidade,
    String uf,
    String ibge,
    String numero,
    String complemento
) {}