package com.br.rkfbank.dto.comum;

import com.br.rkfbank.exceptions.CodigoErroEnum;
import java.time.Instant;
import java.util.List;

// Formato unico de erro da API inteira, montado pelo TratadorGlobalDeErros e pela TratadorErrosSeguranca.
// timestamp/status/error/message/path seguem o que o Spring ja devolvia; "campos" vem preenchido so na
// validacao. "codigo" (contrato 3.0) e o identificador estavel da causa: o front decide pelo status e, quando
// precisa separar causas do mesmo status, pelo codigo. O message e so para exibir.
public record ErroResponse(
    Instant timestamp,
    int status,
    String error,
    CodigoErroEnum codigo,
    String message,
    String path,
    List<ErroCampoResponse> campos
) {}
