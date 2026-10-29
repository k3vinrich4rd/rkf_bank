package com.br.rkfbank.contas.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Corpo do POST /contas/{contaId}/saque. Mesmas regras de formato do deposito; o saldo e conferido no service (422).
public record SaqueRequestDto(
    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no máximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no máximo 140 caracteres")
    String descricao
) {}