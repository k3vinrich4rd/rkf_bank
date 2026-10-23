package com.br.rkfbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// rkfbank.equipe.senha (EQUIPE_SENHA do .env): senha de login das contas da equipe (Felipe e Renan) criadas pelo
// db/seed/equipe. Vazia = as contas existem, mas ninguem entra nelas. Nunca vai para o Git.
@ConfigurationProperties(prefix = "rkfbank.equipe")
public record EquipePropriedades(String senha) {

    // Sem EQUIPE_SENHA as contas da equipe existem (o seed cria), mas ninguem consegue entrar nelas.
    public boolean configurado() {
        return senha != null && !senha.isBlank();
    }
}
