package com.br.rkfbank;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Feature 4: a especificacao OpenAPI sai do codigo e e publica (a tela do Swagger precisa dela sem token).
// Se uma rota sumir ou o esquema JWT for desligado, este teste avisa antes do Swagger mentir para quem le.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void especificacaoEPublicaETemAsRotasDoContrato() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("RKF Bank API"))
                .andExpect(jsonPath("$.info.version").value("3.0"))
                .andExpect(jsonPath("$.paths['/clientes'].post").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/contas/{contaId}'].get").exists())
                .andExpect(jsonPath("$.paths['/contas/{contaId}/depositos'].post").exists())
                .andExpect(jsonPath("$.paths['/transferencias'].post").exists())
                .andExpect(jsonPath("$.paths['/usuarios/me/senha'].patch").exists())
                .andExpect(jsonPath("$.paths['/contas/transferencia']").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"));
    }

    @Test
    void rotasPublicasNaoPedemTokenNoSwagger() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/clientes'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/transferencias'].post.parameters[?(@.name == 'Idempotency-Key')]").exists());
    }

    @Test
    void telaDoSwaggerAbreSemToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("swagger")));
    }
}
