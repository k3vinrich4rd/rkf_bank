package com.br.rkfbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.contas.Lancamento;
import com.br.rkfbank.contas.comprovante.ComprovanteService;
import com.br.rkfbank.contas.comprovante.PublicadorOutbox;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import tools.jackson.databind.json.JsonMapper;

// Comprovante assincrono (Outbox, V16) com o Spring e o banco de verdade e as contas do seed da equipe:
// o evento nasce na transacao do dinheiro, nao nasce quando a operacao e recusada ou repetida, e sai depois.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:comprovante;MODE=MySQL",
        "rkfbank.equipe.senha=" + ComprovanteIntegracaoTest.SENHA_EQUIPE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComprovanteIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    private static final String CPF_FELLIPE = "12345678909";
    private static final String CONTA_FELLIPE = "5eed0000-0000-4000-8000-000000000014";
    private static final String CONTA_RENAN = "5eed0000-0000-4000-8000-000000000024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PublicadorOutbox publicadorOutbox;

    @Autowired
    private ComprovanteService comprovanteService;

    @Test
    void transferenciaGravaOEventoNaHoraEOComprovanteSaiDepois() throws Exception {
        transferir(login(), UUID.randomUUID().toString(), "25.00").andExpect(status().isNoContent());

        // Assincrono: a API ja respondeu 204, o evento esta na fila e ainda nao foi entregue.
        assertEquals(1, pendentes());
        String payload = jdbcTemplate.queryForObject("SELECT payload FROM tb_outbox", String.class);
        assertThat(payload).contains("TRANSFERENCIA", CONTA_FELLIPE, CONTA_RENAN, "25.00")
                // LGPD: nada de descricao livre, CPF ou nome no evento.
                .doesNotContain("Aluguel", CPF_FELLIPE, "Fellipe");

        assertEquals(1, publicadorOutbox.publicarPendentes());
        assertEquals(0, pendentes());
        // Rodada seguinte nao entrega de novo.
        assertEquals(0, publicadorOutbox.publicarPendentes());
    }

    @Test
    void depositoESaqueTambemGeramComprovante() throws Exception {
        String token = login();
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/depositos", "{\"valor\":10.00}").andExpect(status().isNoContent());
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/saques", "{\"valor\":5.00}").andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForList("SELECT payload FROM tb_outbox ORDER BY criado_em", String.class))
                .hasSize(2)
                .anySatisfy(p -> assertThat(p).contains("DEPOSITO"))
                .anySatisfy(p -> assertThat(p).contains("SAQUE"));
    }

    @Test
    void transferenciaRecusadaNaoGeraComprovante() throws Exception {
        transferir(login(), UUID.randomUUID().toString(), "999999.00").andExpect(status().is(422));

        // O rollback do dinheiro leva o evento junto: nao existe comprovante de algo que nao aconteceu.
        assertEquals(0, total());
    }

    @Test
    void repetirComAMesmaChaveNaoGeraOutroComprovante() throws Exception {
        String token = login();
        String chave = UUID.randomUUID().toString();
        transferir(token, chave, "10.00").andExpect(status().isNoContent());
        transferir(token, chave, "10.00").andExpect(status().isNoContent())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        assertEquals(1, total());
    }

    @Test
    void comprovanteSoNasceDentroDaTransacaoDoDinheiro() {
        // Propagation.MANDATORY: chamado sozinho, fora de transacao, recusa em vez de gravar um evento solto.
        assertThrows(IllegalTransactionStateException.class, () -> comprovanteService.registrar(new Lancamento()));
        assertEquals(0, total());
    }

    private ResultActions transferir(String token, String chave, String valor)
            throws Exception {
        return movimentar(token, "/transferencias", "{\"contaOrigemId\":\"" + CONTA_FELLIPE + "\",\"contaDestinoId\":\""
                + CONTA_RENAN + "\",\"valor\":" + valor + ",\"descricao\":\"Aluguel\"}", chave);
    }

    private ResultActions movimentar(String token, String rota, String corpo)
            throws Exception {
        return movimentar(token, rota, corpo, UUID.randomUUID().toString());
    }

    private ResultActions movimentar(String token, String rota, String corpo,
                                                                          String chave) throws Exception {
        return mockMvc.perform(post(rota).header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private int pendentes() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_outbox WHERE publicado_em IS NULL", Integer.class);
    }

    private int total() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_outbox", Integer.class);
    }

    private String login() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + CPF_FELLIPE + "\",\"senha\":\"" + SENHA_EQUIPE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }
}
