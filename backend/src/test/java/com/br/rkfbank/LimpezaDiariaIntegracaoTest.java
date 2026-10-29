package com.br.rkfbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.br.rkfbank.autenticacao.LimitadorTentativasLogin;
import com.br.rkfbank.autenticacao.RefreshTokenService;
import com.br.rkfbank.compartilhado.config.LimpezaPeriodica;
import com.br.rkfbank.compartilhado.config.RotinaLimpezaDiaria;
import com.br.rkfbank.contas.IdempotenciaService;
import com.br.rkfbank.contas.comprovante.PublicadorOutbox;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// A limpeza diaria com o Spring de verdade: as quatro limpezas (idempotencia e outbox de contas, tentativas de login
// e tokens de autenticacao) se registram como LimpezaPeriodica, e cada uma abre a propria transacao (os DELETE do
// Spring Data e do JdbcTemplate falhariam sem ela).
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:limpeza-diaria;MODE=MySQL")
@ActiveProfiles("test")
class LimpezaDiariaIntegracaoTest {

    @Autowired
    private List<LimpezaPeriodica> limpezas;

    @Autowired
    private RotinaLimpezaDiaria rotina;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void todasAsLimpezasDosModulosEntramNaRotina() {
        assertThat(limpezas).extracting(limpeza -> AopUtils.getTargetClass(limpeza).getSimpleName())
                .containsExactlyInAnyOrder(IdempotenciaService.class.getSimpleName(),
                        LimitadorTentativasLogin.class.getSimpleName(), RefreshTokenService.class.getSimpleName(),
                        PublicadorOutbox.class.getSimpleName());
    }

    @Test
    void aRotinaApagaAJanelaVencidaEMantemAAtual() {
        Instant agora = Instant.now();
        jdbcTemplate.update("INSERT INTO tb_tentativa_login (chave_login, falhas, primeira_falha) VALUES (?, ?, ?)",
                "a".repeat(64), 5, Timestamp.from(agora.minus(2, ChronoUnit.HOURS)));
        jdbcTemplate.update("INSERT INTO tb_tentativa_login (chave_login, falhas, primeira_falha) VALUES (?, ?, ?)",
                "b".repeat(64), 1, Timestamp.from(agora));

        rotina.executar();

        assertThat(jdbcTemplate.queryForList("SELECT chave_login FROM tb_tentativa_login", String.class))
                .containsExactly("b".repeat(64));
    }
}
