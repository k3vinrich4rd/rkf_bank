package com.br.rkfbank.clientes.dto.response;

// Endereco do cliente: CEP, logradouro, bairro, cidade, UF e codigo IBGE vem do ViaCEP; numero e complemento,
// do proprio cliente no cadastro.
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