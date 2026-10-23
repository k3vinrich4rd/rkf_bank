package com.br.rkfbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Refresh token (V15): renovar sem senha, rotacao, deteccao de reuso, logout e queda na troca de senha.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:refresh-token;MODE=MySQL",
        "rkfbank.equipe.senha=" + RefreshTokenIntegracaoTest.SENHA_EQUIPE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RefreshTokenIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    private static final String CPF_RENAN = "98765432100";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void renovaSemSenhaETrocaOTokenDeRenovacao() throws Exception {
        JsonNode login = login();
        JsonNode renovado = renovar(login.get("refreshToken").asString());

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + renovado.get("token").asString()))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertNotEquals(login.get("refreshToken").asString(), renovado.get("refreshToken").asString());
    }

    @Test
    void reusarUmTokenJaUsadoDerrubaTodasAsSessoes() throws Exception {
        String primeiro = login().get("refreshToken").asString();
        String segundo = renovar(primeiro).get("refreshToken").asString();

        // Alguem copiou o primeiro e tenta usar: 401, e o segundo (do dono legitimo) cai junto.
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo(primeiro)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESSAO_EXPIRADA"));
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo(segundo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void depoisDoLogoutOTokenNaoRenovaMais() throws Exception {
        String refresh = login().get("refreshToken").asString();

        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(corpo(refresh)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(corpo(refresh)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trocarASenhaInvalidaOsTokensDeRenovacao() throws Exception {
        JsonNode login = login();
        mockMvc.perform(patch("/usuarios/me/senha").header("Authorization", "Bearer " + login.get("token").asString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA_EQUIPE + "\",\"novaSenha\":\"senha-nova-do-renan\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo(login.get("refreshToken").asString())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESSAO_EXPIRADA"));
    }

    @Test
    void tokenInventadoOuVazio() throws Exception {
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo("inventado")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("refreshToken"));
    }

    private JsonNode login() throws Exception {
        return jsonMapper.readTree(mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + CPF_RENAN + "\",\"senha\":\"" + SENHA_EQUIPE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshExpiraEmSegundos").value(604800))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode renovar(String refresh) throws Exception {
        return jsonMapper.readTree(mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo(refresh)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static String corpo(String refresh) {
        return "{\"refreshToken\":\"" + refresh + "\"}";
    }
}
