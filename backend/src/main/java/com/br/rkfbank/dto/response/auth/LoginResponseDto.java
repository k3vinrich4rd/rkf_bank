package com.br.rkfbank.dto.response.auth;

import com.br.rkfbank.entities.enums.PerfilEnum;
import java.util.UUID;

// clienteId vai junto para o frontend abrir conta (POST /contas/clientes/{clienteId}); e nulo para o ADMIN.
public record LoginResponseDto(
    String token,
    String tipo,
    long expiraEmSegundos,
    UUID clienteId,
    PerfilEnum perfil
) {}
