package com.br.rkfbank.dto.response.conta;

import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Uma linha do extrato. contaOrigemId/contaDestinoId dizem para que lado o dinheiro andou:
// deposito so tem destino, saque so tem origem, transferencia tem os dois.
public record LancamentoResponseDto(
    UUID id,
    TipoLancamentoEnum tipoLancamento,
    BigDecimal valor,
    UUID contaOrigemId,
    UUID contaDestinoId,
    String descricao,
    Instant dataHora
) {}