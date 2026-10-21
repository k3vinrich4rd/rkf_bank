package com.br.rkfbank.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

// rkfbank.jwt.* do application.properties. O segredo vem do .env (JWT_SEGREDO), nunca do codigo.
@ConfigurationProperties(prefix = "rkfbank.jwt")
// segredoAleatorio=true (so no perfil visitante): ignora JWT_SEGREDO e sorteia uma chave forte a cada subida.
// Ninguem conhece a chave, nem quem le o codigo; reiniciou, todo token antigo vira 401 (o H2 tambem zera).
public record JwtPropriedades(String segredo, Duration expiracao, boolean segredoAleatorio) {

    @ConstructorBinding
    public JwtPropriedades {
    }

    public JwtPropriedades(String segredo, Duration expiracao) {
        this(segredo, expiracao, false);
    }

    // HS256 exige chave de pelo menos 256 bits; chave curta e facil de quebrar por forca bruta.
    static final int MIN_BYTES_SEGREDO = 32;
}
