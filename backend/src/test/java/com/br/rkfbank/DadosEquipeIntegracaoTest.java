package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Liga o db/seed/equipe e uma EQUIPE_SENHA de teste para provar o que o guia da equipe promete: a API sobe com as
// contas do Felipe (origem) e do Renan (destino), o login so funciona com a senha do ambiente, as movimentacoes
// sao idempotentes, saldo e extrato batem e o callback afterMigrate roda de novo sem duplicar nem resetar nada.
// H2 com outro nome: o testdb e compartilhado pelos outros testes da mesma JVM.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:dados-equipe;DB_CLOSE_DELAY=-1",
        "rkfbank.equipe.senha=" + DadosEquipeIntegracaoTest.SENHA_EQUIPE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DadosEquipeIntegracaoTest {

    // Senha so deste teste; a de verdade fica no .env da equipe e nunca no Git.
    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_FELIPE = "12345678909";
    static final String CPF_RENAN = "98765432100";
    static final String CLIENTE_FELIPE = "5eed0000-0000-4000-8000-000000000012";
    static final String CONTA_FELIPE = "5eed0000-0000-4000-8000-000000000014";
    static final String CLIENTE_RENAN = "5eed0000-0000-4000-8000-000000000022";
    static final String CONTA_RENAN = "5eed0000-0000-4000-8000-000000000024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void felipeERenanEntramComAEquipeSenha() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_FELIPE));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("987.654.321-00", SENHA_EQUIPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_RENAN));
    }

    @Test
    void outraSenhaNaoEntraNaContaDaEquipe() throws Exception {
        // So a EQUIPE_SENHA do ambiente vale; o Git nao guarda senha nem hash das contas da equipe.
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(CPF_FELIPE, "qualquer-outra-senha")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaUmVeSoAPropriaContaSemeada() throws Exception {
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login(CPF_FELIPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(CONTA_FELIPE))
                .andExpect(jsonPath("$[0].agencia").value("0001"))
                .andExpect(jsonPath("$[0].numeroConta").value("100001"))
                .andExpect(jsonPath("$[0].tipoContaEnum").value("CORRENTE"))
                .andExpect(jsonPath("$[0].ativa").value(true));

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login(CPF_RENAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(CONTA_RENAN))
                .andExpect(jsonPath("$[0].numeroConta").value("100002"));
    }

    @Test
    void saldoInicialTemUmLancamentoQueOExplica() {
        // Lancamento nunca e alterado: da para conferir o valor semeado mesmo depois de outros testes movimentarem.
        assertEquals(0, new BigDecimal("1000.00").compareTo(valorDoDepositoInicial(CONTA_FELIPE)));
        assertEquals(0, new BigDecimal("500.00").compareTo(valorDoDepositoInicial(CONTA_RENAN)));
    }

    @Test
    void transferenciaDoFelipeParaORenanAtualizaOsDoisSaldos() throws Exception {
        String tokenFelipe = login(CPF_FELIPE);
        String tokenRenan = login(CPF_RENAN);
        BigDecimal felipeAntes = saldo(tokenFelipe);
        BigDecimal renanAntes = saldo(tokenRenan);

        transferir(tokenFelipe, UUID.randomUUID().toString(), "200.00")
                .andExpect(status().isNoContent());

        assertEquals(0, felipeAntes.subtract(new BigDecimal("200.00")).compareTo(saldo(tokenFelipe)));
        assertEquals(0, renanAntes.add(new BigDecimal("200.00")).compareTo(saldo(tokenRenan)));
    }

    @Test
    void repetirATransferenciaComAMesmaChaveNaoTiraODinheiroDuasVezes() throws Exception {
        String tokenFelipe = login(CPF_FELIPE);
        String tokenRenan = login(CPF_RENAN);
        String chave = UUID.randomUUID().toString();
        BigDecimal felipeAntes = saldo(tokenFelipe);
        BigDecimal renanAntes = saldo(tokenRenan);
        long lancamentosAntes = contar("tb_lancamento");

        transferir(tokenFelipe, chave, "30.00")
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Idempotency-Replayed"));
        // A rede caiu, o app mandou de novo: mesma chave, mesmo corpo.
        transferir(tokenFelipe, chave, "30.00")
                .andExpect(status().isNoContent())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        assertEquals(0, felipeAntes.subtract(new BigDecimal("30.00")).compareTo(saldo(tokenFelipe)));
        assertEquals(0, renanAntes.add(new BigDecimal("30.00")).compareTo(saldo(tokenRenan)));
        assertEquals(lancamentosAntes + 1, contar("tb_lancamento"));
    }

    @Test
    void mesmaChaveComOutroValorE422ESemMovimentar() throws Exception {
        String tokenFelipe = login(CPF_FELIPE);
        String chave = UUID.randomUUID().toString();
        transferir(tokenFelipe, chave, "5.00").andExpect(status().isNoContent());
        BigDecimal saldoDepois = saldo(tokenFelipe);

        transferir(tokenFelipe, chave, "500.00")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Idempotency-Key ja usada em outra requisicao; gere uma chave nova para esta operacao"));

        assertEquals(0, saldoDepois.compareTo(saldo(tokenFelipe)));
    }

    @Test
    void movimentarSemIdempotencyKeyE400ESemMovimentar() throws Exception {
        String tokenFelipe = login(CPF_FELIPE);
        BigDecimal antes = saldo(tokenFelipe);

        mockMvc.perform(post("/contas/" + CONTA_FELIPE + "/saque").header("Authorization", "Bearer " + tokenFelipe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("Idempotency-Key"));

        assertEquals(0, antes.compareTo(saldo(tokenFelipe)));
    }

    @Test
    void renanNaoMovimentaAContaDoFelipe() throws Exception {
        mockMvc.perform(post("/contas/" + CONTA_FELIPE + "/saque").header("Authorization", "Bearer " + login(CPF_RENAN))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saldoDeCadaContaBateComOExtrato() throws Exception {
        // Movimenta um pouco antes de conciliar, para a prova nao depender so do saldo inicial.
        String tokenFelipe = login(CPF_FELIPE);
        mockMvc.perform(post("/contas/" + CONTA_FELIPE + "/deposito").header("Authorization", "Bearer " + tokenFelipe)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":12.34}"))
                .andExpect(status().isNoContent());
        transferir(tokenFelipe, UUID.randomUUID().toString(), "2.34").andExpect(status().isNoContent());

        // Conciliacao: saldo = entradas (destino) - saidas (origem). Divergencia aqui = dinheiro aparecendo ou sumindo.
        List<Map<String, Object>> divergentes = jdbcTemplate.queryForList("""
                SELECT c.numero_conta, c.saldo,
                       COALESCE((SELECT SUM(l.valor) FROM tb_lancamento l WHERE l.conta_destino_id = c.id), 0)
                     - COALESCE((SELECT SUM(l.valor) FROM tb_lancamento l WHERE l.conta_origem_id = c.id), 0) AS pelo_extrato
                FROM tb_conta c
                WHERE c.saldo <> COALESCE((SELECT SUM(l.valor) FROM tb_lancamento l WHERE l.conta_destino_id = c.id), 0)
                               - COALESCE((SELECT SUM(l.valor) FROM tb_lancamento l WHERE l.conta_origem_id = c.id), 0)
                """);

        assertEquals(List.of(), divergentes);
    }

    @Test
    void rodarOFlywayDeNovoNaoDuplicaNemResetaOsDados() throws Exception {
        String tokenFelipe = login(CPF_FELIPE);
        mockMvc.perform(post("/contas/" + CONTA_FELIPE + "/deposito").header("Authorization", "Bearer " + tokenFelipe)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":1.00}"))
                .andExpect(status().isNoContent());
        BigDecimal saldoAntes = saldo(tokenFelipe);
        long lancamentosAntes = contar("tb_lancamento");

        // Simula uma nova subida da API: o migrate dispara o afterMigrate de novo.
        flyway.migrate();

        assertEquals(2, contar("tb_cliente"));
        assertEquals(2, contar("tb_usuario"));
        assertEquals(2, contar("tb_conta"));
        assertEquals(lancamentosAntes, contar("tb_lancamento"));
        assertEquals(0, saldoAntes.compareTo(saldo(tokenFelipe)), "o seed nunca reseta saldo ja movimentado");
    }

    @Test
    void historicoDoFlywayTemSoAsVersoesDoSchema() {
        // O callback afterMigrate nao entra no flyway_schema_history: so as migracoes versionadas.
        List<String> versoes = Arrays.stream(flyway.info().applied())
                .map(migracao -> migracao.getVersion().getVersion())
                .toList();

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), versoes);
    }

    private ResultActions transferir(String token, String chave, String valor) throws Exception {
        return mockMvc.perform(post("/contas/transferencia").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaOrigemId\":\"" + CONTA_FELIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN
                        + "\",\"valor\":" + valor + ",\"descricao\":\"Aluguel\"}"));
    }

    private String login(String cpf) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, SENHA_EQUIPE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }

    private BigDecimal saldo(String token) throws Exception {
        String corpo = mockMvc.perform(get("/contas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode conta = jsonMapper.readTree(corpo).get(0);
        return conta.get("saldo").decimalValue();
    }

    private BigDecimal valorDoDepositoInicial(String contaId) {
        return jdbcTemplate.queryForObject(
                "SELECT valor FROM tb_lancamento WHERE conta_destino_id = ? AND descricao = 'Aporte inicial do idealizador'",
                BigDecimal.class, uuidEmBytes(contaId));
    }

    private long contar(String tabela) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }

    private static byte[] uuidEmBytes(String uuid) {
        UUID u = UUID.fromString(uuid);
        return ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();
    }

    private static String loginJson(String cpf, String senha) {
        return "{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}";
    }
}
