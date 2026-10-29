package com.br.rkfbank.mysql;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.clientes.viacep.ViaCepClient;
import com.br.rkfbank.clientes.viacep.ViaCepResponseDto;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

// Base dos testes contra um MySQL 8.4 DE VERDADE (feature 3: Testcontainers), a mesma imagem do docker-compose.yml.
//
// Por que existe: o resto da suite roda no H2, que e rapido mas nao e o banco de producao (o CHECK das colunas de enum,
// o BINARY(16) dos UUIDs e o lock otimista se comportam diferente). Aqui as migracoes do Flyway rodam no MySQL e a API
// inteira funciona em cima dele.
//
// Como funciona: o container e um @Bean com @ServiceConnection; o Spring Boot le a URL, o usuario e a senha dele e
// troca o datasource do perfil test sozinho. Todas as classes que herdam daqui tem a mesma configuracao, entao o Spring
// reaproveita o mesmo contexto e o MESMO container (ele sobe uma vez por execucao, nao uma vez por classe).
// Sem Docker na maquina, as classes sao puladas com aviso (disabledWithoutDocker); no CI (GitHub Actions) sempre rodam.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MySqlIntegracaoBase.ContainerMySql.class)
@Testcontainers(disabledWithoutDocker = true)
abstract class MySqlIntegracaoBase {

    static final String SENHA = "senha-forte-1";

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerMySql {

        @Bean
        @ServiceConnection
        MySQLContainer mysql() {
            // Sem TLS: banco descartavel, local, so do teste. Com TLS, o certificado que o MySQL gera na subida pode
            // nascer "no futuro" quando o relogio da VM do Docker esta adiantado, e o driver recusa a conexao.
            return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                    .withUrlParam("useSSL", "false")
                    .withUrlParam("allowPublicKeyRetrieval", "true");
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    // ViaCEP e externo: o teste prova o NOSSO banco, nao a internet.
    @MockitoBean
    ViaCepClient viaCepClient;

    @BeforeEach
    void viaCepDeOsasco() {
        when(viaCepClient.buscarPorCep(anyString())).thenReturn(new ViaCepResponseDto(
                "06020-010", "Avenida dos Autonomistas", "Vila Yara", "Osasco", "SP", "3534401", false));
    }

    // Cadastra um cliente novo (CPF valido sorteado) e devolve o token dele.
    String clienteNovoComToken() throws Exception {
        String cpf = cpfValidoAleatorio();
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomeCompleto":"Cliente MySQL","cpf":"%s","email":"%s@exemplo.test","telefone":"(11) 99999-8888",
                         "cep":"06020-010","numero":"123","senha":"%s","aceiteTermos":true}""".formatted(cpf, cpf, SENHA)))
                .andExpect(status().isCreated());
        return login(cpf);
    }

    String login(String cpf) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }

    String abrirConta(String token, String tipo) throws Exception {
        String corpo = mockMvc.perform(post("/contas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipoConta\":\"" + tipo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("id").asString();
    }

    void depositar(String token, String contaId, String valor) throws Exception {
        mockMvc.perform(post("/contas/" + contaId + "/depositos").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":" + valor + "}"))
                .andExpect(status().isNoContent());
    }

    static String cpfValidoAleatorio() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (d[0] == d[1] && d[1] == d[2]);
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) {
                soma += d[i] * (tamanho + 1 - i);
            }
            int resto = (soma * 10) % 11;
            d[tamanho] = resto == 10 ? 0 : resto;
        }
        StringBuilder cpf = new StringBuilder();
        for (int digito : d) {
            cpf.append(digito);
        }
        return cpf.toString();
    }
}
