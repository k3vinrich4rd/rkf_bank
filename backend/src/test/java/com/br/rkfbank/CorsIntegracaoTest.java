package com.br.rkfbank;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// CORS de verdade, passando pela cadeia do Spring Security: o navegador manda o preflight (OPTIONS) sem token,
// e ele precisa ser respondido antes da autenticacao. As rotas nao tem prefixo /api.
@SpringBootTest(properties = "rkfbank.cors.origens-permitidas=http://localhost:3000,https://front.rkfbank.test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightDeOrigemPermitidaNaRotaDeContasRespondeSemToken() throws Exception {
        mockMvc.perform(options("/contas/transferencia")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("idempotency-key")));
    }

    @Test
    void segundaOrigemDaListaTambemEPermitida() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "https://front.rkfbank.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://front.rkfbank.test"));
    }

    @Test
    void preflightDeOrigemDesconhecidaERecusado() throws Exception {
        mockMvc.perform(options("/contas")
                        .header("Origin", "https://site-malicioso.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void respostaParaOrigemPermitidaExpoeOsHeadersQueOFrontPrecisaLer() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Request-Id")))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Idempotency-Replayed")));
    }
}
