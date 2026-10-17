package com.br.rkfbank.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// rkfbank.jwt.* do application.properties. O segredo vem do .env (JWT_SEGREDO), nunca do codigo.
@ConfigurationProperties(prefix = "rkfbank.jwt")
public record JwtPropriedades(String segredo, Duration expiracao) {

    // HS256 exige chave de pelo menos 256 bits; chave curta e facil de quebrar por forca bruta.
    static final int MIN_BYTES_SEGREDO = 32;
}
