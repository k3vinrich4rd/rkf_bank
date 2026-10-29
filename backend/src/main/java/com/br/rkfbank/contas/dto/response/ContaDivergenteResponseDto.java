package com.br.rkfbank.contas.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

// Uma conta cujo saldo NAO bate com o extrato (entradas - saidas). GET /admin/conciliacao devolve so estas;
// lista vazia e o normal. Qualquer linha aqui e bug ou alteracao manual no banco: investigar antes de mexer.
public record ContaDivergenteResponseDto(
    UUID contaId,
    String numeroConta,
    BigDecimal saldo,
    BigDecimal saldoPeloExtrato
) {}
