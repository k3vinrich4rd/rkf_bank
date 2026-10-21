package com.br.rkfbank.dto.request.usuario;

import com.br.rkfbank.entities.enums.PerfilEnum;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequestDto(
    @NotNull(message = "perfil e obrigatorio e deve ser CLIENTE ou ADMIN")
    PerfilEnum perfil
) {}
