package com.br.rkfbank.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// As migracoes do Flyway no PostgreSQL 17 real. O contexto so sobe se o Hibernate validar o schema criado por elas
// (ddl-auto=validate), entao chegar aqui ja prova que entidade e tabela batem no banco de producao.
class MigracoesPostgresTest extends PostgresIntegracaoBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bancoEPostgres17() {
        String versao = jdbcTemplate.queryForObject("SHOW server_version", String.class);
        assertTrue(versao.startsWith("17"), versao);
    }

    @Test
    void todasAsMigracoesRodaramComSucessoEmOrdem() {
        List<String> versoes = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
                String.class);
        List<String> esperadas = new java.util.ArrayList<>();
        for (int v = 1; v <= ultimaMigracao(); v++) {
            esperadas.add(String.valueOf(v));
        }
        assertEquals(esperadas, versoes);
    }

    @Test
    void colunasDeDinheiroUuidEDataTemOTipoDeProducao() {
        assertEquals("numeric(19,2)", tipoDaColuna("tb_conta", "saldo"));
        assertEquals("uuid", tipoDaColuna("tb_conta", "id"));
        assertEquals("bigint", tipoDaColuna("tb_conta", "versao"));
        assertEquals("boolean", tipoDaColuna("tb_conta", "ativa"));
        assertEquals("timestamp(6) with time zone", tipoDaColuna("tb_lancamento", "data_hora"));
    }

    // format_type devolve o tipo como o psql mostra (numeric(19,2), timestamp(6) with time zone).
    private String tipoDaColuna(String tabela, String coluna) {
        return jdbcTemplate.queryForObject(
                "SELECT format_type(a.atttypid, a.atttypmod) FROM pg_attribute a "
                        + "WHERE a.attrelid = ?::regclass AND a.attname = ?", String.class, tabela, coluna);
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
