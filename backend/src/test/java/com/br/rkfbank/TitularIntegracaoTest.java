package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.clients.viacep.ViaCepClient;
import com.br.rkfbank.clients.viacep.ViaCepResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.repositories.ClienteRepository;
import com.br.rkfbank.repositories.LancamentoRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.services.TitularService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

// Direitos do titular (LGPD) de ponta a ponta com o seed da equipe: o Felipe ve, corrige e encerra o proprio
// cadastro; a rotina anonimiza quando o prazo de retencao vence. DirtiesContext: cada teste com o banco do seed intacto.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        "spring.datasource.url=jdbc:h2:mem:titular",
        "rkfbank.equipe.senha=" + TitularIntegracaoTest.SENHA_EQUIPE,
        "rkfbank.admin.cpf=" + TitularIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + TitularIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TitularIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";

    private static final String CPF_FELIPE = "12345678909";
    private static final String CPF_RENAN = "98765432100";
    private static final UUID CLIENTE_FELIPE = UUID.fromString("5eed0000-0000-4000-8000-000000000012");
    private static final String CONTA_FELIPE = "5eed0000-0000-4000-8000-000000000014";
    private static final String CONTA_RENAN = "5eed0000-0000-4000-8000-000000000024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TitularService titularService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    // ViaCEP e externo: a correcao de endereco usa a resposta simulada.
    @MockitoBean
    private ViaCepClient viaCepClient;

    // ---------- ver ----------

    @Test
    void titularVeOsPropriosDadosPeloToken() throws Exception {
        mockMvc.perform(get("/clientes/me").header("Authorization", "Bearer " + login(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENTE_FELIPE.toString()))
                .andExpect(jsonPath("$.nomeCompleto").value(org.hamcrest.Matchers.startsWith("Felipe Santana")))
                .andExpect(jsonPath("$.endereco.cidade").value("Osasco"));
    }

    @Test
    void semTokenE401EAdminSemCadastroE422() throws Exception {
        mockMvc.perform(get("/clientes/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/clientes/me").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Seu usuario nao tem cadastro de cliente"));
    }

    // ---------- corrigir ----------

    @Test
    void titularCorrigeEmailETelefoneENaoMexeNoCpf() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        corrigir(token, "{\"email\":\"Felipe.Novo@Exemplo.test\",\"telefone\":\"(11) 91234-5678\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("felipe.novo@exemplo.test"))
                .andExpect(jsonPath("$.telefone").value("(11) 91234-5678"))
                .andExpect(jsonPath("$.cpf").value("123.456.789-09"));

        mockMvc.perform(get("/clientes/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.email").value("felipe.novo@exemplo.test"));
    }

    @Test
    void novoCepBuscaOEnderecoNoViaCepEExigeONumero() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        when(viaCepClient.buscarPorCep(anyString())).thenReturn(
                new ViaCepResponseDto("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308", null));

        corrigir(token, "{\"cep\":\"01001000\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("numero"));

        corrigir(token, "{\"cep\":\"01001-000\",\"numero\":\"200\",\"complemento\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.cep").value("01001-000"))
                .andExpect(jsonPath("$.endereco.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.endereco.numero").value("200"))
                .andExpect(jsonPath("$.endereco.complemento").doesNotExist());
    }

    @Test
    void emailDeOutroClienteE409ECorpoVazioE400() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        String emailDoRenan = jsonMapper.readTree(mockMvc.perform(get("/clientes/me")
                        .header("Authorization", "Bearer " + login(CPF_RENAN, SENHA_EQUIPE)))
                .andReturn().getResponse().getContentAsString()).get("email").asString();

        corrigir(token, "{\"email\":\"" + emailDoRenan + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email ja cadastrado"));
        corrigir(token, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("corpo"));
        corrigir(token, "{\"email\":\"sem-arroba\"}")
                .andExpect(status().isBadRequest());
    }

    // ---------- encerrar ----------

    @Test
    void naoEncerraComSaldoNemComSenhaErrada() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        encerrar(token, "chute-errado")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Senha incorreta"));
        encerrar(token, SENHA_EQUIPE)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Transfira o saldo das suas contas antes de encerrar o cadastro"));
        // Nada mudou: o Felipe continua entrando e movimentando.
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void encerrarDesativaContasELoginNaHoraEInformaAteQuandoGuarda() throws Exception {
        String token = encerrarFelipeComSaldoZerado();

        // Token antigo cai na hora e o login nao entra mais.
        mockMvc.perform(get("/clientes/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + CPF_FELIPE + "\",\"senha\":\"" + SENHA_EQUIPE + "\"}"))
                .andExpect(status().isUnauthorized());

        // Conta encerrada nao recebe mais dinheiro.
        mockMvc.perform(post("/contas/transferencia").header("Authorization", "Bearer " + login(CPF_RENAN, SENHA_EQUIPE))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":\"" + CONTA_RENAN + "\",\"contaDestinoId\":\"" + CONTA_FELIPE + "\",\"valor\":1.00}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Conta inativa"));
    }

    @Test
    void adminNaoReativaQuemEncerrouOCadastro() throws Exception {
        encerrarFelipeComSaldoZerado();
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);
        UUID usuarioFelipe = usuarioRepository.findByLogin(CPF_FELIPE).orElseThrow().getId();

        mockMvc.perform(patch("/admin/usuarios/" + usuarioFelipe + "/acesso").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":true}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("O titular encerrou o cadastro; o acesso nao pode ser reativado"));
    }

    // ---------- anonimizar ----------

    @Test
    void rotinaSoAnonimizaDepoisDoPrazoEGuardaOsLancamentos() throws Exception {
        encerrarFelipeComSaldoZerado();
        long lancamentosAntes = lancamentoRepository.count();

        // Dentro do prazo (5 anos): nada acontece.
        assertEquals(0, titularService.anonimizarVencidos(Instant.now().plus(365, ChronoUnit.DAYS)));
        assertTrue(clienteRepository.findById(CLIENTE_FELIPE).orElseThrow().getNomeCompleto().startsWith("Felipe Santana"));

        // Prazo vencido: anonimiza uma vez so.
        Instant daquiASeisAnos = Instant.now().plus(6 * 366, ChronoUnit.DAYS);
        assertEquals(1, titularService.anonimizarVencidos(daquiASeisAnos));
        assertEquals(0, titularService.anonimizarVencidos(daquiASeisAnos));

        Cliente anonimo = clienteRepository.findById(CLIENTE_FELIPE).orElseThrow();
        assertEquals("Titular anonimizado", anonimo.getNomeCompleto());
        assertTrue(anonimo.getCpf().startsWith("A"));
        assertEquals(11, anonimo.getCpf().length());
        assertTrue(anonimo.getEmail().endsWith("@rkfbank.invalid"));
        assertEquals("anonimizado", anonimo.getTelefone());
        assertEquals("00000-000", anonimo.getEndereco().getCep());
        assertNotNull(anonimo.getAnonimizadoEm());

        Usuario credencial = usuarioRepository.findByClienteId(CLIENTE_FELIPE).orElseThrow();
        assertEquals(anonimo.getCpf(), credencial.getLogin());
        assertFalse(credencial.isAtivo());
        assertTrue(usuarioRepository.findByLogin(CPF_FELIPE).isEmpty());

        // Registro financeiro fica: nenhum lancamento foi apagado.
        assertEquals(lancamentosAntes, lancamentoRepository.count());
    }

    // O Felipe passa todo o saldo para o Renan e encerra. Devolve o token que ele usou.
    private String encerrarFelipeComSaldoZerado() throws Exception {
        String token = login(CPF_FELIPE, SENHA_EQUIPE);
        String saldo = jsonMapper.readTree(mockMvc.perform(get("/contas").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get(0).get("saldo").asString();
        mockMvc.perform(post("/contas/transferencia").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":\"" + CONTA_FELIPE + "\",\"contaDestinoId\":\"" + CONTA_RENAN + "\",\"valor\":" + saldo + "}"))
                .andExpect(status().isNoContent());

        encerrar(token, SENHA_EQUIPE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encerradoEm").isNotEmpty())
                .andExpect(jsonPath("$.anonimizacaoPrevistaPara").isNotEmpty())
                .andExpect(jsonPath("$.motivoDaRetencao").value(org.hamcrest.Matchers.containsString("Lei 9.613/1998")));
        return token;
    }

    // ---------- apoio ----------

    private ResultActions corrigir(String token, String corpo) throws Exception {
        return mockMvc.perform(patch("/clientes/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions encerrar(String token, String senha) throws Exception {
        return mockMvc.perform(post("/clientes/me/encerramento").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"" + senha + "\"}"));
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }
}
