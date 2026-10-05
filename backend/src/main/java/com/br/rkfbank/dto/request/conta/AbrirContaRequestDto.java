package com.br.rkfbank.dto.request.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AbrirContaRequestDto(
    @NotBlank(message = "agencia e obrigatoria")
    @Pattern(regexp = "\\d{4}", message = "agencia deve ter 4 digitos")
    String agencia,

    @NotBlank(message = "numeroConta e obrigatorio")
    @Pattern(regexp = "\\d{5,12}", message = "numeroConta deve ter de 5 a 12 digitos")
    String numeroConta,

    @NotNull(message = "tipoContaEnum e obrigatorio e deve ser CORRENTE ou POUPANCA")
    TipoContaEnum tipoContaEnum
) {}