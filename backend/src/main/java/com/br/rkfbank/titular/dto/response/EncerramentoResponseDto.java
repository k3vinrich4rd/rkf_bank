package com.br.rkfbank.titular.dto.response;

import java.time.Instant;
import java.time.LocalDate;

// Transparencia (LGPD, art. 9): o titular sabe ate quando os dados ficam guardados e por que.
public record EncerramentoResponseDto(
    Instant encerradoEm,
    LocalDate anonimizacaoPrevistaPara,
    String motivoDaRetencao
) {}
