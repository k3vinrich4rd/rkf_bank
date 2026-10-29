package com.br.rkfbank.contas.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

// Corpo do POST /transferencias. As duas contas vem no corpo porque a operacao envolve duas;
// o service confere que a de ORIGEM e do usuario logado (403) e que a de destino existe e esta ativa.
public record TransferenciaRequestDto(
    @NotNull(message = "contaOrigemId é obrigatória")
    UUID contaOrigemId,

    @NotNull(message = "contaDestinoId é obrigatória")
    UUID contaDestinoId,

    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no máximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no máximo 140 caracteres")
    String descricao
) {}