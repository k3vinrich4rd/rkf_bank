package com.br.rkfbank.clients.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseViaCepDto(
    String cep,
    String logradouro,
    String bairro,
    String localidade,
    String uf,
    String ibge,
    Boolean erro
) {}