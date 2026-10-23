package com.br.rkfbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

// Contrato 3.0: so as rotas canonicas existem; as do contrato 2.x respondem 404. Usa as contas do seed da equipe.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:rotas-contrato",
        "rkfbank.equipe.senha=" + RotasContratoIntegracaoTest.SENHA_EQUIPE,
        "rkfbank.admin.cpf=" + RotasContratoIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + RotasContratoIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RotasContratoIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";
    private static final String CPF_FELLIPE = "12345678909";
    private static final String CLIENTE_FELLIPE = "5eed0000-0000-4000-8000-000000000012";
    private static final String CONTA_FELLIPE = "5eed0000-0000-4000-8000-000000000014";
    private static final String CONTA_RENAN = "5eed0000-0000-4000-8000-000000000024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void abreContaNoProprioCadastroSoComOTipo() throws Exception {
        mockMvc.perform(post("/contas").header("Authorization", "Bearer " + login(CPF_FELLIPE, SENHA_EQUIPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoConta\":\"POUPANCA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_FELLIPE))
                .andExpect(jsonPath("$.tipoConta").value("POUPANCA"))
                .andExpect(jsonPath("$.agencia").value("0001"));
    }

    @Test
    void nomeAntigoDoCampoTipoContaEnumNaoEMaisAceito() throws Exception {
        mockMvc.perform(post("/contas").header("Authorization", "Bearer " + login(CPF_FELLIPE, SENHA_EQUIPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoContaEnum\":\"POUPANCA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("tipoConta"));
    }

    @Test
    void adminSemCadastroNaoAbreConta() throws Exception {
        mockMvc.perform(post("/contas").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoConta\":\"CORRENTE\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("SEM_CADASTRO_CLIENTE"))
                .andExpect(jsonPath("$.message").value("Seu usuário não tem cadastro de cliente"));
    }

    @Test
    void movimentacoesPelasRotasDoContrato() throws Exception {
        String token = login(CPF_FELLIPE, SENHA_EQUIPE);
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/depositos", "{\"valor\":10.00}").andExpect(status().isNoContent());
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/saques", "{\"valor\":5.00}").andExpect(status().isNoContent());
        movimentar(token, "/transferencias",
                "{\"contaOrigemId\":\"" + CONTA_FELLIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN + "\",\"valor\":1.00}")
                .andExpect(status().isNoContent());
    }

    // Contrato 3.0: as rotas do contrato 2.x sairam. Nenhuma delas responde nem move dinheiro.
    @Test
    void rotasRetiradasNoContrato3NaoExistemMais() throws Exception {
        String token = login(CPF_FELLIPE, SENHA_EQUIPE);
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/deposito", "{\"valor\":10.00}").andExpect(status().isNotFound());
        movimentar(token, "/contas/" + CONTA_FELLIPE + "/saque", "{\"valor\":5.00}").andExpect(status().isNotFound());
        movimentar(token, "/contas/transferencia",
                "{\"contaOrigemId\":\"" + CONTA_FELLIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN + "\",\"valor\":1.00}")
                // 405 e nao 404: /contas/{contaId} existe (GET), so nao aceita POST. Nos dois casos nada se move.
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/contas/clientes/" + CLIENTE_FELLIPE).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoConta\":\"POUPANCA\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/auth/senha").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA_EQUIPE + "\",\"novaSenha\":\"senha-nova-do-fellipe\"}"))
                .andExpect(status().isNotFound());

        // O saldo semeado do Fellipe continua intacto e ele segue com uma conta so.
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].saldo").value(1000.00));
    }

    @Test
    void trocaDeSenhaPelaRotaDoUsuario() throws Exception {
        mockMvc.perform(patch("/usuarios/me/senha").header("Authorization", "Bearer " + login(CPF_FELLIPE, SENHA_EQUIPE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA_EQUIPE + "\",\"novaSenha\":\"senha-nova-do-fellipe\"}"))
                .andExpect(status().isNoContent());
        login(CPF_FELLIPE, "senha-nova-do-fellipe");
    }

    @Test
    void pageOuSizeNaUrlPedemAPaginaEParametroPaginadoEIgnorado() throws Exception {
        String admin = login(CPF_ADMIN, SENHA_ADMIN);
        mockMvc.perform(get("/clientes").param("size", "1").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1));
        mockMvc.perform(get("/contas").param("page", "0").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
        // paginado=true saiu no contrato 3.0: sem page/size, vem a lista inteira.
        mockMvc.perform(get("/clientes").param("paginado", "true").header("Authorization", "Bearer " + admin))
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
