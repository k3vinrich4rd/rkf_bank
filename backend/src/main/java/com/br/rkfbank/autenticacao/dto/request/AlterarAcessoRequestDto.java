package com.br.rkfbank.autenticacao.dto.request;

import jakarta.validation.constraints.NotNull;

// ativo=false desativa o login e derruba os tokens ja emitidos; ativo=true devolve o acesso.
public record AlterarAcessoRequestDto(
    @NotNull(message = "ativo é obrigatório (true ou false)")
    Boolean ativo
) {}
