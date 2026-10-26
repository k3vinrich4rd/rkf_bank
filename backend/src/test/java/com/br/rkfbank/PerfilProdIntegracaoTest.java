package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// O perfil prod de verdade (application-prod.properties), com o MySQL trocado por um H2 proprio so para o teste.
// Prova as garantias do README_DEPLOY.md: nenhum dado de teste no banco, Swagger fechado, login da equipe nunca
// criado (mesmo com EQUIPE_SENHA no ambiente) e CORS so para o dominio do site.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:perfil-prod;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "JWT_SEGREDO=segredo-de-producao-de-teste-com-mais-de-32-bytes",
        "CORS_ORIGENS=" + PerfilProdIntegracaoTest.SITE,
        // Se o provedor tiver estas variaveis por engano, o prod nao pode criar o login da equipe.
        "EQUIPE_SENHA=senha-da-equipe-do-ambiente"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class PerfilProdIntegracaoTest {

    static final String SITE = "https://rkfbank.example";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void bancoNasceSoComOSchemaSemNenhumDadoDeTeste() {
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_cliente", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_conta", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_usuario", Long.class));
    }

    @Test
    void loginDaEquipeNaoExisteMesmoComASenhaNoAmbiente() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\",\"senha\":\"senha-da-equipe-do-ambiente\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerFechadoEHealthAberto() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsSoParaODominioDoSite() throws Exception {
        mockMvc.perform(options("/contas").header("Origin", SITE).header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
        // O localhost:3000 do padrao de desenvolvimento nao vale em producao.
        mockMvc.perform(options("/contas").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
