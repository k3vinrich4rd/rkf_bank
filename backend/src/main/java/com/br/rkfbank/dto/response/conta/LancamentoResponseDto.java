package com.br.rkfbank.dto.response.conta;

import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LancamentoResponseDto(
    UUID id,
    TipoLancamentoEnum tipoLancamento,
    BigDecimal valor,
    UUID contaOrigemId,
    UUID contaDestinoId,
    String descricao,
    Instant dataHora
) {}