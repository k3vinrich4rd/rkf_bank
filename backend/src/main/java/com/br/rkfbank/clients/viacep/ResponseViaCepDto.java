package com.br.rkfbank.clients.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Resposta do ViaCEP (https://viacep.com.br/ws/{cep}/json/), com os nomes que ELE usa ("localidade" = cidade).
// ignoreUnknown: o ViaCEP manda mais campos (ddd, siafi, gia...) e pode criar outros; nada disso quebra a leitura.
// "erro": true chega com status 200 quando o CEP nao existe; o ClienteViaCep transforma isso em 422.
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