package com.br.rkfbank.dto.response.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import java.math.BigDecimal;
import java.util.UUID;

public record ContaResponseDto(
    UUID id,
    UUID clienteId,
    String agencia,
    String numeroConta,
    TipoContaEnum tipoContaEnum,
    BigDecimal saldo,
    boolean ativa
) {}
