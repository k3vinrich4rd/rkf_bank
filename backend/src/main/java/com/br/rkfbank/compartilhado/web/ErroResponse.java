package com.br.rkfbank.compartilhado.web;

import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
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
) {

    // Record e imutavel so na "casca": a lista recebida continuaria mutavel por quem guardou a referencia.
    // List.copyOf faz a copia defensiva (e devolve uma lista que recusa add/remove); null vira lista vazia,
    // que e o que o contrato promete em "campos".
    public ErroResponse {
        campos = campos == null ? List.of() : List.copyOf(campos);
    }
}
