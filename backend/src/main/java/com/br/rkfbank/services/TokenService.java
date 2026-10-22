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
    public static final String CLAIM_VERSAO_CREDENCIAL = "versaoCredencial";

    private final JwtEncoder jwtEncoder;
    private final JwtPropriedades propriedades;

    public TokenService(JwtEncoder jwtEncoder, JwtPropriedades propriedades) {
        this.jwtEncoder = jwtEncoder;
        this.propriedades = propriedades;
    }

    // Emite o token depois que o AutenticacaoService conferiu CPF e senha. Claims (dados dentro do token):
    //  iss = quem emitiu ("rkfbank") · sub = id do USUARIO (nao o CPF) · iat/exp = emitido em / vence em
    //  perfil = CLIENTE ou ADMIN · clienteId = de quem sao as contas · versaoCredencial = muda quando a senha muda.
    // Quem garante que ninguem trocou "CLIENTE" por "ADMIN" no payload e a assinatura HMAC-SHA256 com o segredo.
    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracao()))
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                // Trocou a senha depois deste login? A versao no banco fica maior e este token deixa de valer.
                .claim(CLAIM_VERSAO_CREDENCIAL, usuario.getVersaoCredencial());
        // O ADMIN nao e cliente: o token dele nao leva clienteId.
        if (usuario.getCliente() != null) {
            claims.claim(CLAIM_CLIENTE_ID, usuario.getCliente().getId().toString());
        }

        // Sem o header HS256 explicito o encoder procura uma chave RSA e falha.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenEmitido(token, propriedades.expiracao().toSeconds());
    }

    // O token e quantos segundos ele vale, para o front saber quando a sessao vai cair.
    public record TokenEmitido(String token, long expiraEmSegundos) {
    }
}
