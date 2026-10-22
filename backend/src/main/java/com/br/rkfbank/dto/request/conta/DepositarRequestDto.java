package com.br.rkfbank.dto.request.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Corpo do POST /contas/{contaId}/deposito. A conta vem da URL e o dono vem do token: aqui so o valor.
// @DecimalMin + @Digits barram zero, negativo e mais de 2 casas ANTES de chegar ao service (400).
// BigDecimal e nao double: 0.1 + 0.2 em double da 0.30000000000000004; dinheiro precisa ser exato.
public record DepositarRequestDto(
    @NotNull(message = "valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no maximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no maximo 140 caracteres")
    String descricao
) {}