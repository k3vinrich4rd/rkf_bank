package com.br.rkfbank.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.exceptions.MuitasTentativasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// Contra o banco de verdade do teste (H2 com as migracoes do Flyway, incluindo a V12 da tb_tentativa_login).
@DataJpaTest
@ActiveProfiles("test")
class LimitadorTentativasLoginTest {

    private static final String CPF = "52998224725";

    // Relogio que o teste avanca na mao: testa a janela de 15 min sem esperar 15 min.
    private static final class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-10-20T12:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RelogioManual relogio = new RelogioManual();
    private LimitadorTentativasLogin limitador;

    @BeforeEach
    void setUp() {
        // O H2 do perfil test (jdbc:h2:mem:testdb) e o mesmo dos @SpringBootTest: se o SegurancaIntegracaoTest rodar
        // antes, os logins errados dele ficam aqui. Comeca da tabela vazia; o @DataJpaTest desfaz no fim do teste.
        jdbcTemplate.update("DELETE FROM tb_tentativa_login");
        limitador = new LimitadorTentativasLogin(jdbcTemplate, relogio);
    }

    @Test
    void quatroErrosAindaDeixamTentar() {
        errar(LimitadorTentativasLogin.MAX_FALHAS - 1);

        assertDoesNotThrow(() -> limitador.conferir(CPF));
    }

    @Test
    void quintoErroBloqueiaComOsSegundosQueFaltam() {
        errar(LimitadorTentativasLogin.MAX_FALHAS);
        relogio.avancar(Duration.ofMinutes(5));

        MuitasTentativasException erro = assertThrows(MuitasTentativasException.class, () -> limitador.conferir(CPF));
        // Janela de 15 min, 5 ja passaram: faltam ~600 s.
        assertTrue(erro.getSegundosParaLiberar() > 590 && erro.getSegundosParaLiberar() <= 600);
    }

    @Test
    void bloqueioAcabaQuandoAJanelaPassa() {
        errar(LimitadorTentativasLogin.MAX_FALHAS);
        relogio.avancar(LimitadorTentativasLogin.JANELA);

        assertDoesNotThrow(() -> limitador.conferir(CPF));
    }

    @Test
    void acertarASenhaZeraOContador() {
        errar(LimitadorTentativasLogin.MAX_FALHAS - 1);
        limitador.limpar(CPF);
        errar(LimitadorTentativasLogin.MAX_FALHAS - 1);

        assertDoesNotThrow(() -> limitador.conferir(CPF));
    }

    @Test
    void bloqueioDeUmCpfNaoAtingeOutro() {
        errar(LimitadorTentativasLogin.MAX_FALHAS);

        assertDoesNotThrow(() -> limitador.conferir("11144477735"));
    }

    @Test
    void falhaDepoisDaJanelaVencidaComecaContagemNova() {
        errar(LimitadorTentativasLogin.MAX_FALHAS - 1);
        relogio.avancar(LimitadorTentativasLogin.JANELA.plusSeconds(1));
        errar(LimitadorTentativasLogin.MAX_FALHAS - 1);

        assertDoesNotThrow(() -> limitador.conferir(CPF));
    }

    @Test
    void cpfNaoFicaNaTabelaSoOHashEJanelasVencidasSaem() {
        errar(1);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_tentativa_login WHERE chave_login LIKE ?", Integer.class, "%" + CPF + "%"));
        assertEquals(64, jdbcTemplate.queryForObject("SELECT chave_login FROM tb_tentativa_login", String.class).length());

        relogio.avancar(LimitadorTentativasLogin.JANELA.plusSeconds(1));
        assertEquals(1, limitador.apagarJanelasVencidas());
    }

    private void errar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            limitador.registrarFalha(CPF);
        }
    }
}
