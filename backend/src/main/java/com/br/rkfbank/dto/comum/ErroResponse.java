package com.br.rkfbank.dto.comum;

import java.time.Instant;
import java.util.List;

// Formato unico de erro da API inteira, montado pelo ControllerExceptionHandler e pela RespostaErroSeguranca.
// timestamp/status/error/message/path seguem o que o Spring ja devolvia; "campos" vem preenchido so na
// validacao. Com um formato so, o front trata qualquer erro do mesmo jeito: decide pelo status, mostra a message.
public record ErroResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ErroCampoResponse> campos
) {}

