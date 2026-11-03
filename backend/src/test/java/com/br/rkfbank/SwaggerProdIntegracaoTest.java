package com.br.rkfbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// A demo publica liga o Swagger com SWAGGER_ATIVO=true: o contrato fica legivel, mas so para leitura.
// O PerfilProdIntegracaoTest continua provando que, sem a variavel, ele fica fechado.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:swagger-prod;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "JWT_SEGREDO=segredo-de-producao-de-teste-com-mais-de-32-bytes",
        "CORS_ORIGENS=https://rkfbank.example",
        "SWAGGER_ATIVO=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class SwaggerProdIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerAbertoSoParaLeitura() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("RKF Bank API"));
        // Lista vazia = a tela nao mostra o "Try it out" em nenhuma rota.
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportedSubmitMethods").isEmpty());
    }
}
