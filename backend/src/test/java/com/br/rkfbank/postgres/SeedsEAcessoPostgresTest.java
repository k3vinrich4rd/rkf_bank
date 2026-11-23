package com.br.rkfbank.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.br.rkfbank.exceptions.MuitasTentativasException;
import com.br.rkfbank.security.LimitadorTentativasLogin;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

// Os dois seeds (equipe e visitante) em PostgreSQL de verdade: eles usam CAST(... AS UUID), INSERT ... SELECT com NULL
// e datas com fuso, que sao os pontos onde o PostgreSQL e mais exigente que o H2. E o limitador de login, que fala
// SQL direto (JdbcTemplate) com timestamptz. Sao os dois lugares do projeto fora do JPA.
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe,classpath:db/seed/visitante")
class SeedsEAcessoPostgresTest extends PostgresIntegracaoBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private LimitadorTentativasLogin limitador;

    @Test
    void seedsCriamAsQuatroContasComSaldoEExtratoQueBatem() {
        assertEquals(4, contar("SELECT COUNT(*) FROM tb_conta"));
        assertEquals(4, contar("SELECT COUNT(*) FROM tb_lancamento"));
        assertEquals(0, contar("SELECT COUNT(*) FROM tb_conta c WHERE c.saldo <> "
                + "COALESCE((SELECT SUM(valor) FROM tb_lancamento WHERE conta_destino_id = c.id), 0)"));
        assertEquals(new BigDecimal("1000.00"), jdbc.queryForObject(
                "SELECT saldo FROM tb_conta WHERE numero_conta = '100001'", BigDecimal.class));
        // Os visitantes tem login (BCrypt publico); a equipe nao (a senha vem do ambiente).
        assertEquals(2, contar("SELECT COUNT(*) FROM tb_usuario"));
    }

    @Test
    void rodarOsSeedsDeNovoNaoDuplicaNada() {
        flyway.migrate();
        flyway.migrate();

        assertEquals(4, contar("SELECT COUNT(*) FROM tb_conta"));
        assertEquals(4, contar("SELECT COUNT(*) FROM tb_cliente"));
    }

    @Test
    void limiteDeTentativasDeLoginBloqueiaNaQuintaFalhaELimpaNoSucesso() {
        String cpf = "11144477735";
        for (int i = 0; i < 4; i++) {
            limitador.registrarFalha(cpf);
        }
        limitador.conferir(cpf); // 4 falhas ainda passam
        limitador.registrarFalha(cpf);

        assertThrows(MuitasTentativasException.class, () -> limitador.conferir(cpf));
        assertEquals(1, contar("SELECT COUNT(*) FROM tb_tentativa_login WHERE falhas = 5"));

        limitador.limpar(cpf);
        limitador.conferir(cpf);
        assertEquals(0, contar("SELECT COUNT(*) FROM tb_tentativa_login"));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
