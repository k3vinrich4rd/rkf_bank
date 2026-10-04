package com.br.rkfbank.dto.request.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferirRequestDto(
    @NotNull(message = "contaOrigemId e obrigatoria")
    UUID contaOrigemId,

    @NotNull(message = "contaDestinoId e obrigatoria")
    UUID contaDestinoId,

    @NotNull(message = "valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no maximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no maximo 140 caracteres")
    String descricao
) {}