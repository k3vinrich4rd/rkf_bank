package com.br.rkfbank.dto.request.usuario;

import com.br.rkfbank.entities.enums.PerfilEnum;
import jakarta.validation.constraints.NotNull;

// PATCH /admin/usuarios/{id}/perfil. Valor fora do enum (ex.: "GERENTE") nem chega aqui: o Jackson falha ao
// converter e o handler responde 400 "JSON invalido ou tipo de campo incompativel".
public record AlterarPerfilRequestDto(
    @NotNull(message = "perfil é obrigatório e deve ser CLIENTE ou ADMIN")
    PerfilEnum perfil
) {}
