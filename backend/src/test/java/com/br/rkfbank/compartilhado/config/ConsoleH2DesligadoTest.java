package com.br.rkfbank.compartilhado.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Como no perfil padrao (MySQL): console desligado. A ConfiguracaoConsoleH2 nao e criada e o caminho do console
// vira uma rota qualquer da API, que exige token.
@SpringBootTest(properties = "spring.h2.console.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsoleH2DesligadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void consoleNaoFicaLiberadoSemAPropriedade() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }
}
