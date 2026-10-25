package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.config.ConfiguracaoSeguranca;
import com.br.rkfbank.config.JwtPropriedades;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-ok";

    private final ConfiguracaoSeguranca configuracao = new ConfiguracaoSeguranca();
    private final JwtPropriedades propriedades = new JwtPropriedades(SEGREDO, Duration.ofMinutes(30));
    private final SecretKey chave = configuracao.chaveJwt(propriedades);
    private final JwtDecoder decoder = configuracao.jwtDecoder(chave);
    private final TokenService tokenService = new TokenService(configuracao.jwtEncoder(chave), propriedades);

    @Test
    void deveGerarTokenAssinadoComPerfilClienteIdEExpiracao() {
        Usuario usuario = usuario(PerfilEnum.CLIENTE, UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e"));

        TokenService.TokenEmitido gerado = tokenService.emitir(usuario);
        Jwt jwt = decoder.decode(gerado.token());

        assertEquals(1800, gerado.expiraEmSegundos());
        assertEquals(usuario.getId().toString(), jwt.getSubject());
        assertEquals("rkfbank", jwt.getClaimAsString("iss"));
        assertEquals("CLIENTE", jwt.getClaimAsString(TokenService.CLAIM_PERFIL));
        assertEquals("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e", jwt.getClaimAsString(TokenService.CLAIM_CLIENTE_ID));
        // Versao da senha vai no token: trocou a senha, a versao no banco sobe e este token passa a ser recusado.
        assertEquals(0, ((Number) jwt.getClaim(TokenService.CLAIM_VERSAO_CREDENCIAL)).intValue());
        assertEquals(Duration.ofMinutes(30), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals("HS256", jwt.getHeaders().get("alg").toString());
    }

    @Test
    void tokenDoAdminNaoLevaClienteId() {
        Jwt jwt = decoder.decode(tokenService.emitir(usuario(PerfilEnum.ADMIN, null)).token());

        assertEquals("ADMIN", jwt.getClaimAsString(TokenService.CLAIM_PERFIL));
        assertNull(jwt.getClaimAsString(TokenService.CLAIM_CLIENTE_ID));
    }

    @Test
    void payloadNaoLevaCpf() {
        String token = tokenService.emitir(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())).token();

        // O payload e so Base64: qualquer um decodifica sem a chave.
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertTrue(payload.contains("\"perfil\":\"CLIENTE\""));
        assertFalse(payload.contains("52998224725"));
    }

    @Test
    void deveRecusarTokenComPayloadAlterado() {
        String token = tokenService.emitir(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())).token();
        String[] partes = token.split("\\.");
        String payloadOriginal = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadAdmin = payloadOriginal.replace("\"CLIENTE\"", "\"ADMIN\"");
        String adulterado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payloadAdmin.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        assertThrows(JwtException.class, () -> decoder.decode(adulterado));
    }

    @Test
    void deveRecusarTokenAssinadoComOutraChave() {
        JwtPropriedades outras = new JwtPropriedades("outro-segredo-qualquer-com-mais-de-32-bytes", Duration.ofMinutes(30));
        TokenService outroEmissor = new TokenService(configuracao.jwtEncoder(configuracao.chaveJwt(outras)), outras);

        String tokenDeFora = outroEmissor.emitir(usuario(PerfilEnum.ADMIN, null)).token();

        assertThrows(JwtException.class, () -> decoder.decode(tokenDeFora));
    }

    @Test
    void naoDeveSubirComSegredoCurto() {
        // Valor que nao se confunde com nenhuma palavra da mensagem, para provar que o segredo nao vaza nela.
        JwtPropriedades fraco = new JwtPropriedades("x9Kq2", Duration.ofHours(1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configuracao.chaveJwt(fraco));

        assertTrue(ex.getMessage().contains("JWT_SEGREDO"));
        assertFalse(ex.getMessage().contains("x9Kq2"));
    }

    @Test
    void naoDeveSubirSemSegredo() {
        assertThrows(IllegalStateException.class,
                () -> configuracao.chaveJwt(new JwtPropriedades(null, Duration.ofHours(1))));
    }

    @Test
    void segredoAleatorioIgnoraOSegredoConfiguradoEMudaACadaSubida() {
        // Perfil visitante: ninguem conhece a chave, nem quem le o application-visitante.properties.
        JwtPropriedades visitante = new JwtPropriedades(null, Duration.ofHours(1), true);

        byte[] primeira = configuracao.chaveJwt(visitante).getEncoded();
        byte[] segunda = configuracao.chaveJwt(visitante).getEncoded();

        assertEquals(48, primeira.length);
        assertFalse(java.util.Arrays.equals(primeira, segunda));
    }

    private Usuario usuario(PerfilEnum perfil, UUID clienteId) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setLogin("52998224725");
        usuario.setPerfil(perfil);
        if (clienteId != null) {
            Cliente cliente = new Cliente();
            ReflectionTestUtils.setField(cliente, "id", clienteId);
            usuario.setCliente(cliente);
        }
        return usuario;
    }
}
