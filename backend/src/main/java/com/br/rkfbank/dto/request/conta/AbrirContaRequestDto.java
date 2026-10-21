package com.br.rkfbank.dto.request.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// agencia e numeroConta sao opcionais desde o contrato 2.1: sem eles o banco usa a agencia 0001 e GERA o numero
// (com digito verificador). Ainda aceitos para nao quebrar quem ja manda; o @Pattern so vale quando vier valor.
public record AbrirContaRequestDto(
    @Pattern(regexp = "\\d{4}", message = "agencia deve ter 4 digitos")
    String agencia,

    @Pattern(regexp = "\\d{5,12}", message = "numeroConta deve ter de 5 a 12 digitos")
    String numeroConta,

    @NotNull(message = "tipoContaEnum e obrigatorio e deve ser CORRENTE ou POUPANCA")
    TipoContaEnum tipoContaEnum
) {}
