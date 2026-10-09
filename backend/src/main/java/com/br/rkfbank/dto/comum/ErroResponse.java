package com.br.rkfbank.dto.comum;

import java.time.Instant;
import java.util.List;

public record ErroResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ErroCampoResponse> campos
) {}

