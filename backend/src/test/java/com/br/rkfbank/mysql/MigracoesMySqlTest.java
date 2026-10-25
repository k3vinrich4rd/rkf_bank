package com.br.rkfbank.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// As migracoes do Flyway no MySQL 8.4 real. O contexto so sobe se o Hibernate validar o schema criado por elas
// (ddl-auto=validate), entao chegar aqui ja prova que entidade e tabela batem no banco de producao.
class MigracoesMySqlTest extends MySqlIntegracaoBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bancoEMySql84() {
        String versao = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertTrue(versao.startsWith("8.4"), versao);
    }

    @Test
    void todasAsMigracoesRodaramComSucessoEmOrdem() {
        List<String> versoes = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank",
                String.class);
        List<String> esperadas = new java.util.ArrayList<>();
        for (int v = 1; v <= ultimaMigracao(); v++) {
            esperadas.add(String.valueOf(v));
        }
        assertEquals(esperadas, versoes);
    }

    @Test
    void colunasDeDinheiroEUuidTemOTipoDeProducao() {
        assertEquals("decimal(19,2)", tipoDaColuna("tb_conta", "saldo"));
        assertEquals("binary(16)", tipoDaColuna("tb_conta", "id"));
        assertEquals("bigint", tipoDaColuna("tb_conta", "versao"));
    }

    private String tipoDaColuna(String tabela, String coluna) {
        return jdbcTemplate.queryForObject(
                "SELECT LOWER(column_type) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = ? AND column_name = ?", String.class, tabela, coluna);
    }

    // A ultima V<n> da pasta db/migration: o teste acompanha migracao nova sem precisar ser editado.
    private static int ultimaMigracao() {
        try (var arquivos = java.nio.file.Files.list(java.nio.file.Path.of("backend/src/main/resources/db/migration"))) {
            return arquivos.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("V\\d+__.*\\.sql"))
                    .mapToInt(n -> Integer.parseInt(n.substring(1, n.indexOf("__"))))
                    .max().orElseThrow();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
