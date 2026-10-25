package com.br.rkfbank.services;

import com.br.rkfbank.entities.RefreshToken;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.exceptions.SessaoExpiradaException;
import com.br.rkfbank.repositories.RefreshTokenRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Token de renovacao (refresh token). O JWT de acesso vive 15 min e nao pode ser revogado (ninguem guarda estado
// dele); o de renovacao vive mais (7 dias) e mora no banco, entao pode ser revogado no logout, na troca de senha e
// quando alguem tenta reusar um velho.
//
// Rotacao: cada renovacao revoga o token usado e emite outro. Se um token JA revogado aparece de novo, alguem o
// copiou (o dono legitimo ja recebeu o sucessor): todos os tokens daquele usuario caem e ele entra com a senha.
@Service
public class RefreshTokenService {

    private static final Logger log = LogManager.getLogger(RefreshTokenService.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final Duration validade;

    public RefreshTokenService(RefreshTokenRepository repository, UsuarioRepository usuarioRepository,
                               @Value("${rkfbank.jwt.refresh-validade:P7D}") Duration validade) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.validade = validade;
    }

    // Emite um token novo para o usuario. Devolve o VALOR (vai para o cliente uma unica vez); o banco guarda o hash.
    @Transactional
    public Emitido emitir(Usuario usuario) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(usuario.getId(), hash(valor), usuario.getVersaoCredencial(),
                Instant.now().plus(validade)));
        return new Emitido(valor, validade.toSeconds());
    }

    // Troca um token valido pelo usuario dono (e revoga o usado). Qualquer problema -> 401 SESSAO_EXPIRADA, sem
    // dizer qual: quem tenta adivinhar token nao aprende nada.
    @Transactional(noRollbackFor = SessaoExpiradaException.class)
    public Usuario consumir(String valor) {
        Instant agora = Instant.now();
        RefreshToken token = repository.findByHashToken(hash(valor)).orElseThrow(() -> {
            log.warn("Renovacao recusada: token desconhecido");
            return new SessaoExpiradaException();
        });
        if (token.isRevogado()) {
            // Reuso: o legitimo ja trocou este token. Derruba todos (o commit acontece mesmo com a excecao).
            int derrubados = repository.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            log.warn("Reuso de refresh token do usuario {}: {} sessao(oes) revogada(s)", token.getUsuarioId(), derrubados);
            throw new SessaoExpiradaException();
        }
        token.revogar(agora);
        Optional<Usuario> usuario = usuarioRepository.findById(token.getUsuarioId());
        if (!agora.isBefore(token.getExpiraEm()) || usuario.isEmpty() || !usuario.get().isAtivo()
                || usuario.get().getVersaoCredencial() != token.getVersaoCredencial()) {
            log.warn("Renovacao recusada: token vencido, usuario desativado ou senha trocada (usuario {})", token.getUsuarioId());
            throw new SessaoExpiradaException();
        }
        return usuario.get();
    }

    // Logout: revoga o token informado. Desconhecido ou ja revogado: nada a fazer (sair duas vezes nao e erro).
    @Transactional
    public void revogar(String valor) {
        repository.findByHashToken(hash(valor)).ifPresent(token -> token.revogar(Instant.now()));
    }

    // Rotina diaria: tira do banco os vencidos ha mais de 30 dias.
    @Transactional
    public int apagarVencidos(Instant agora) {
        return repository.apagarVencidosAntesDe(agora.minus(Duration.ofDays(30)));
    }

    static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }

    public record Emitido(String valor, long expiraEmSegundos) {
    }
}
