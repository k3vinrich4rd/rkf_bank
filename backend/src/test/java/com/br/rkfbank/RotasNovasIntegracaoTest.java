package com.br.rkfbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

// Contrato 2.6: rotas canonicas no padrao de mercado e as antigas ainda funcionando, marcadas com
// Deprecation + Link (RFC 9745 e RFC 8288). Usa as contas do seed da equipe.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:rotas-novas",
        "rkfbank.equipe.senha=" + RotasNovasIntegracaoTest.SENHA_EQUIPE,
        "rkfbank.admin.cpf=" + RotasNovasIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + RotasNovasIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RotasNovasIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";
    private static final String CPF_FELIPE = "12345678909";
    private static final String CLIENTE_FELIPE = "5eed0000-0000-4000-8000-000000000012";
    private static final String CONTA_FELIPE = "5eed0000-0000-4000-8000-000000000014";
    private static final String CONTA_RENAN = "5eed0000-0000-4000-8000-000000000024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void abreContaNoProprioCadastroComONomeDeMercado() throws Exception {
        mockMvc.perform(post("/contas").header("Authorization", "Bearer " + login(CPF_FELIPE, SENHA_EQUIPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoConta\":\"POUPANCA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_FELIPE))
                .andExpect(jsonPath("$.tipoContaEnum").value("POUPANCA"))
                .andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    void adminSemCadastroNaoAbreContaPelaRotaNova() throws Exception {
        mockMvc.perform(post("/contas").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoContaEnum\":\"CORRENTE\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Seu usuario nao tem cadastro de cliente"));
    }

    @Test
    void movimentacoesPelasRotasNovasNaoTemAvisoDeObsoleta() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        movimentar(token, "/contas/" + CONTA_FELIPE + "/depositos", "{\"valor\":10.00}")
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Deprecation"));
        movimentar(token, "/contas/" + CONTA_FELIPE + "/saques", "{\"valor\":5.00}")
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Deprecation"));
        movimentar(token, "/transferencias",
                "{\"contaOrigemId\":\"" + CONTA_FELIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN + "\",\"valor\":1.00}")
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    void rotasAntigasContinuamFuncionandoEApontamASucessora() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        movimentar(token, "/contas/" + CONTA_FELIPE + "/deposito", "{\"valor\":10.00}")
                .andExpect(status().isNoContent())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</contas/" + CONTA_FELIPE + "/depositos>; rel=\"successor-version\""));
        movimentar(token, "/contas/" + CONTA_FELIPE + "/saque", "{\"valor\":5.00}")
                .andExpect(status().isNoContent())
                .andExpect(header().string("Link", "</contas/" + CONTA_FELIPE + "/saques>; rel=\"successor-version\""));
        movimentar(token, "/contas/transferencia",
                "{\"contaOrigemId\":\"" + CONTA_FELIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN + "\",\"valor\":1.00}")
                .andExpect(status().isNoContent())
                .andExpect(header().string("Link", "</transferencias>; rel=\"successor-version\""));
        mockMvc.perform(post("/contas/clientes/" + CLIENTE_FELIPE).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoContaEnum\":\"POUPANCA\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Link", "</contas>; rel=\"successor-version\""));
    }

    @Test
    void trocaDeSenhaPelaRotaNovaEPelaAntiga() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        mockMvc.perform(patch("/usuarios/me/senha").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA_EQUIPE + "\",\"novaSenha\":\"senha-nova-do-felipe\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Deprecation"));
        mockMvc.perform(patch("/auth/senha").header("Authorization", "Bearer " + login(CPF_FELIPE, "senha-nova-do-felipe"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-nova-do-felipe\",\"novaSenha\":\"outra-senha-do-felipe\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Link", "</usuarios/me/senha>; rel=\"successor-version\""));
    }

    @Test
    void pageOuSizeNaUrlJaPedemAPaginaSemOParametroPaginado() throws Exception {
        String admin = login(CPF_ADMIN, SENHA_ADMIN);
        mockMvc.perform(get("/clientes").param("size", "1").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1));
        mockMvc.perform(get("/contas").param("page", "0").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private ResultActions movimentar(String token, String rota, String corpo) throws Exception {
        return mockMvc.perform(post(rota).header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }
}
