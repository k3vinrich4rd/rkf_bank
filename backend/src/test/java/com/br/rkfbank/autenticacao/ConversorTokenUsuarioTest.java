package com.br.rkfbank.autenticacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.compartilhado.erros.AcessoAlteradoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

class ConversorTokenUsuarioTest {

    private static final UUID ID = UUID.randomUUID();

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);

    @Test
    void semCacheConfereNoBancoACadaRequisicao() {
        ConversorTokenUsuario conversor = new ConversorTokenUsuario(repositorio, Duration.ZERO, Clock.systemUTC());
        when(repositorio.findById(ID)).thenReturn(Optional.of(usuario(false)));

        conversor.convert(token(0));
        conversor.convert(token(0));

        verify(repositorio, times(2)).findById(ID);
    }

    @Test
    void comCacheReaproveitaDentroDoPrazoEConsultaDepois() {
        Clock relogio = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);
        ConversorTokenUsuario conversor = new ConversorTokenUsuario(repositorio, Duration.ofSeconds(5), relogio);
        when(repositorio.findById(ID)).thenReturn(Optional.of(usuario(false)));

        conversor.convert(token(0));
        conversor.convert(token(0));
        verify(repositorio, times(1)).findById(ID);

        ReflectionTestUtils.setField(conversor, "clock", Clock.offset(relogio, Duration.ofSeconds(6)));
        conversor.convert(token(0));
        verify(repositorio, times(2)).findById(ID);
    }

    // O preco do cache, documentado: com ele ligado, a revogacao (aqui, troca de senha) so vale quando o prazo vence.
    @Test
    void comCacheARevogacaoValeQuandoOPrazoVence() {
        Clock relogio = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);
        ConversorTokenUsuario conversor = new ConversorTokenUsuario(repositorio, Duration.ofSeconds(5), relogio);
        Usuario usuario = usuario(false);
        when(repositorio.findById(ID)).thenReturn(Optional.of(usuario));
        conversor.convert(token(0));

        usuario.trocarSenha("novo-hash");
        conversor.convert(token(0)); // ainda dentro dos 5 s: o resultado guardado vale

        ReflectionTestUtils.setField(conversor, "clock", Clock.offset(relogio, Duration.ofSeconds(6)));
        assertThrows(AcessoAlteradoException.class, () -> conversor.convert(token(0)));
    }

    @Test
    void semCacheARevogacaoValeNaHora() {
        ConversorTokenUsuario conversor = new ConversorTokenUsuario(repositorio, Duration.ZERO, Clock.systemUTC());
        Usuario usuario = usuario(false);
        when(repositorio.findById(ID)).thenReturn(Optional.of(usuario));
        conversor.convert(token(0));

        usuario.trocarSenha("novo-hash");

        assertThrows(AcessoAlteradoException.class, () -> conversor.convert(token(0)));
    }

    @Test
    void senhaTemporariaSoGanhaAAutoridadeDeTrocarASenha() {
        ConversorTokenUsuario conversor = new ConversorTokenUsuario(repositorio, Duration.ZERO, Clock.systemUTC());
        when(repositorio.findById(ID)).thenReturn(Optional.of(usuario(true)));

        AbstractAuthenticationToken autenticacao = conversor.convert(token(1));

        assertEquals(1, autenticacao.getAuthorities().size());
        assertEquals(ConversorTokenUsuario.SENHA_TEMPORARIA, autenticacao.getAuthorities().iterator().next().getAuthority());
    }

    private static Usuario usuario(boolean senhaTemporaria) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", ID);
        usuario.setPerfil(PerfilEnum.CLIENTE);
        if (senhaTemporaria) {
            usuario.redefinirSenhaTemporaria("hash-temporario");
        }
        return usuario;
    }

    private static Jwt token(int versaoCredencial) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(ID.toString())
                .claim(TokenService.CLAIM_PERFIL, "CLIENTE")
                .claim(TokenService.CLAIM_VERSAO_CREDENCIAL, versaoCredencial)
                .build();
    }
}
