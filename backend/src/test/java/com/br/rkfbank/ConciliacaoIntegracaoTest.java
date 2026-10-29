package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.contas.IdempotenciaService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import tools.jackson.databind.json.JsonMapper;

// Conciliacao (GET /admin/conciliacao) e limpeza da idempotencia, com as contas do seed da equipe.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:conciliacao;MODE=MySQL",
        "rkfbank.equipe.senha=" + ConciliacaoIntegracaoTest.SENHA_EQUIPE,
        "rkfbank.admin.cpf=" + ConciliacaoIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + ConciliacaoIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConciliacaoIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";
    private static final String CONTA_FELLIPE = "5eed0000-0000-4000-8000-000000000014";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdempotenciaService idempotenciaService;

    @Test
    void saldosDoSeedBatemComOExtrato() throws Exception {
        mockMvc.perform(get("/admin/conciliacao").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void saldoMexidoNaMaoApareceComoDivergente() throws Exception {
        jdbcTemplate.update("UPDATE tb_conta SET saldo = saldo + 1 WHERE numero_conta = '100001'");

        mockMvc.perform(get("/admin/conciliacao").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contaId").value(CONTA_FELLIPE))
                .andExpect(jsonPath("$[0].saldo").value(1001.00))
                .andExpect(jsonPath("$[0].saldoPeloExtrato").value(1000.00));
    }

    @Test
    void clienteNaoVeAConciliacao() throws Exception {
        mockMvc.perform(get("/admin/conciliacao").header("Authorization", "Bearer " + login("12345678909", SENHA_EQUIPE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void limpezaApagaSoAsChavesComMaisDe30Dias() throws Exception {
        String token = login("12345678909", SENHA_EQUIPE);
        mockMvc.perform(post("/contas/" + CONTA_FELLIPE + "/depositos").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":1.00}"))
                .andExpect(status().isNoContent());

        assertEquals(0, idempotenciaService.apagarChavesAntigas(Instant.now()));
        assertEquals(1, idempotenciaService.apagarChavesAntigas(Instant.now().plus(31, ChronoUnit.DAYS)));
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }
}
