package com.br.rkfbank.dto.request.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// agencia e numeroConta sao opcionais desde o contrato 2.1: sem eles o banco usa a agencia 0001 e GERA o numero
// (com digito verificador). Ainda aceitos para nao quebrar quem ja manda; o @Pattern so vale quando vier valor.
public record AberturaContaRequestDto(
    @Pattern(regexp = "\\d{4}", message = "agencia deve ter 4 digitos")
    String agencia,

    @Pattern(regexp = "\\d{5,12}", message = "numeroConta deve ter de 5 a 12 digitos")
    String numeroConta,

    // "tipoConta" e o nome que o mercado usaria (o JSON nao conta como o Java guarda o dado); aceito desde a 2.6.
    // A resposta continua com tipoContaEnum ate o contrato 3.0, para nao quebrar o frontend.
    @JsonAlias("tipoConta")

    @NotNull(message = "tipoContaEnum e obrigatorio e deve ser CORRENTE ou POUPANCA")
    TipoContaEnum tipoContaEnum
) {}
