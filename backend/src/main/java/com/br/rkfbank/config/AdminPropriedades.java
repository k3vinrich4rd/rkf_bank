package com.br.rkfbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// rkfbank.admin.* (ADMIN_CPF e ADMIN_SENHA do .env). Vazios = nenhum admin criado.
@ConfigurationProperties(prefix = "rkfbank.admin")
public record AdminPropriedades(String cpf, String senha) {

    public boolean configurado() {
        return cpf != null && !cpf.isBlank() && senha != null && !senha.isBlank();
    }
}
