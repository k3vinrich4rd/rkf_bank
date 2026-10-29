package com.br.rkfbank.autenticacao;

import com.br.rkfbank.compartilhado.erros.MuitasTentativasException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Freia o chute de senha: 5 logins errados para o mesmo CPF dentro de 15 min bloqueiam aquele CPF ate a janela
// acabar (429). Acertou a senha, o contador zera.
//
// Fica no BANCO (tb_tentativa_login, V12): reiniciar a API nao zera, e varias instancias contam juntas. O CPF nunca
// vai para a tabela nem para o log, so o SHA-256 dele.
//
// Cada metodo roda na SUA transacao (REQUIRES_NEW): o login e readOnly e, quando a senha esta errada, termina em
// excecao; na mesma transacao, a falha que acabou de ser contada seria desfeita junto.
@Component
public class LimitadorTentativasLogin {

    private static final Logger log = LogManager.getLogger(LimitadorTentativasLogin.class);

    static final int MAX_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public LimitadorTentativasLogin(JdbcTemplate jdbc) {
        this(jdbc, Clock.systemUTC());
    }

    // Construtor dos testes: com um Clock fixo, o teste "avanca o relogio" 15 min sem esperar de verdade.
    LimitadorTentativasLogin(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // Chamado ANTES de conferir a senha: bloqueado nao gasta BCrypt nem descobre se a senha estava certa.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void conferir(String login) {
        List<Janela> janelas = jdbc.query("SELECT falhas, primeira_falha FROM tb_tentativa_login WHERE chave_login = ?",
                (rs, linha) -> new Janela(rs.getInt(1), rs.getTimestamp(2).toInstant()), chave(login));
        if (janelas.isEmpty()) {
            return;
        }
        Janela atual = janelas.getFirst();
        Instant fimDaJanela = atual.primeira().plus(JANELA);
        Instant agora = clock.instant();
        if (agora.isBefore(fimDaJanela) && atual.falhas() >= MAX_FALHAS) {
            long segundos = Math.max(1, Duration.between(agora, fimDaJanela).toSeconds());
            log.warn("Login bloqueado por excesso de tentativas ({} falhas); libera em {} s", atual.falhas(), segundos);
            throw new MuitasTentativasException(segundos);
        }
    }

    // Senha errada: soma 1 dentro da janela, ou comeca uma janela nova se a anterior ja venceu. A conta e feita
    // pelo proprio banco num UPDATE so (atomico): duas tentativas ao mesmo tempo nao leem o mesmo numero e perdem
    // uma. Se a linha ainda nao existe, INSERT; se outra requisicao inseriu primeiro, volta para o UPDATE.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(String login) {
        String chave = chave(login);
        Timestamp agora = Timestamp.from(clock.instant());
        Timestamp inicioDaJanelaValida = Timestamp.from(clock.instant().minus(JANELA));
        if (somarFalha(chave, agora, inicioDaJanelaValida) > 0) {
            return;
        }
        try {
            jdbc.update("INSERT INTO tb_tentativa_login (chave_login, falhas, primeira_falha) VALUES (?, 1, ?)", chave, agora);
        } catch (DuplicateKeyException corrida) {
            somarFalha(chave, agora, inicioDaJanelaValida);
        }
    }

    // Login certo: esquece os erros daquele CPF.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void limpar(String login) {
        jdbc.update("DELETE FROM tb_tentativa_login WHERE chave_login = ?", chave(login));
    }

    // Rotina diaria: janelas que ja venceram nao bloqueiam ninguem e so ocupam espaco.
    @Transactional
    public int apagarJanelasVencidas() {
        return jdbc.update("DELETE FROM tb_tentativa_login WHERE primeira_falha < ?", Timestamp.from(clock.instant().minus(JANELA)));
    }

    private int somarFalha(String chave, Timestamp agora, Timestamp inicioDaJanelaValida) {
        return jdbc.update("""
                UPDATE tb_tentativa_login
                   SET falhas = CASE WHEN primeira_falha >= ? THEN falhas + 1 ELSE 1 END,
                       primeira_falha = CASE WHEN primeira_falha >= ? THEN primeira_falha ELSE ? END
                 WHERE chave_login = ?""", inicioDaJanelaValida, inicioDaJanelaValida, agora, chave);
    }

    private static String chave(String login) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(login.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }

    private record Janela(int falhas, Instant primeira) {
    }
}
