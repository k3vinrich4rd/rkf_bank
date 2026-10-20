package com.br.rkfbank.dto.response.usuario;

import com.br.rkfbank.entities.enums.PerfilEnum;
import java.util.UUID;

// O que a gestao de usuarios precisa para decidir, e nada alem (LGPD): CPF mascarado, nunca hash nem senha.
// nome e clienteId vem nulos para o ADMIN que nao e cliente.
public record UsuarioAdminResponseDto(
    UUID id,
    String cpf,
    String nome,
    PerfilEnum perfil,
    boolean ativo,
    UUID clienteId
) {}
