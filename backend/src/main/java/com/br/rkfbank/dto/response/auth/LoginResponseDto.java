package com.br.rkfbank.dto.response.auth;

import com.br.rkfbank.entities.enums.PerfilEnum;
import java.util.UUID;

// clienteId vai junto para o frontend saber quais contas sao do titular (o ADMIN ve todas); e nulo para o ADMIN do .env.
public record LoginResponseDto(
    String token,
    String tipo,
    long expiraEmSegundos,
    UUID clienteId,
    PerfilEnum perfil
) {}
