package com.br.rkfbank.services;

import com.br.rkfbank.config.JwtPropriedades;
import com.br.rkfbank.entities.Usuario;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Monta e assina o JWT. O payload e so Base64 (qualquer um le no jwt.io): nada de CPF, senha ou dado pessoal aqui.
@Service
public class TokenService {

    public static final String EMISSOR = "rkfbank";
    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_CLIENTE_ID = "clienteId";

    private final JwtEncoder jwtEncoder;
    private final JwtPropriedades propriedades;

    public TokenService(JwtEncoder jwtEncoder, JwtPropriedades propriedades) {
        this.jwtEncoder = jwtEncoder;
        this.propriedades = propriedades;
    }

    public TokenGerado gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracao()))
                .claim(CLAIM_PERFIL, usuario.getPerfil().name());
        // O ADMIN nao e cliente: o token dele nao leva clienteId.
        if (usuario.getCliente() != null) {
            claims.claim(CLAIM_CLIENTE_ID, usuario.getCliente().getId().toString());
        }

        // Sem o header HS256 explicito o encoder procura uma chave RSA e falha.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenGerado(token, propriedades.expiracao().toSeconds());
    }

    public record TokenGerado(String token, long expiraEmSegundos) {
    }
}
