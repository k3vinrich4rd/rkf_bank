package com.br.rkfbank;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.clientes.viacep.ViaCepClient;
import com.br.rkfbank.clientes.viacep.ViaCepResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// LGPD (V10): o cadastro so existe com o aceite dos termos, e o banco guarda qual versao foi aceita e quando.
@SpringBootTest(properties = "rkfbank.lgpd.termos-versao=2026-10-23")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TermosIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        when(viaCepClient.buscarPorCep(anyString())).thenReturn(new ViaCepResponseDto(
                "06020-010", "Avenida dos Autonomistas", "Vila Yara", "Osasco", "SP", "3534401", false));
    }

    @Test
    void versaoVigenteEPublica() throws Exception {
        mockMvc.perform(get("/termos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value("2026-10-23"));
    }

    @Test
    void cadastroComAceiteRegistraVersaoEData() throws Exception {
        String cpf = cpfLivre();
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastro(cpf, ",\"aceiteTermos\":true,\"termosVersao\":\"2026-10-23\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**"))
                .andExpect(jsonPath("$.termosVersao").value("2026-10-23"))
                .andExpect(jsonPath("$.termosAceitosEm").isString());
    }

    @Test
    void cadastroSemAceiteDa400NoCampo() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastro("39053344705", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("aceiteTermos"));
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastro("39053344705", ",\"aceiteTermos\":false")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("aceiteTermos"));
    }

    @Test
    void cadastroComTermosDeOutraVersaoDa422() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastro(cpfLivre(), ",\"aceiteTermos\":true,\"termosVersao\":\"2020-01-01\"")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("TERMOS_DESATUALIZADOS"));
    }

    // CPF valido que nenhum outro teste usa (o H2 em memoria e compartilhado entre as classes do mesmo contexto).
    private static String cpfLivre() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = java.util.concurrent.ThreadLocalRandom.current().nextInt(10);
        }
        for (int t = 9; t <= 10; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += d[i] * (t + 1 - i);
            }
            d[t] = (soma * 10) % 11 == 10 ? 0 : (soma * 10) % 11;
        }
        StringBuilder cpf = new StringBuilder();
        for (int x : d) {
            cpf.append(x);
        }
        return cpf.toString();
    }

    private static String cadastro(String cpf, String aceite) {
        return "{\"nomeCompleto\":\"Cliente Termos\",\"cpf\":\"" + cpf + "\",\"email\":\"t" + cpf + "@exemplo.test\","
                + "\"telefone\":\"11999998888\",\"cep\":\"06020010\",\"numero\":\"1\",\"senha\":\"senha-forte-1\"" + aceite + "}";
    }
}
