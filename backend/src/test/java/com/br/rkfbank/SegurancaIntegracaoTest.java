package com.br.rkfbank;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.clients.viacep.ClienteViaCep;
import com.br.rkfbank.clients.viacep.ResponseViaCepDto;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.services.TokenService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

// Cadeia de filtros de verdade (Spring inteiro + H2 + Flyway), com tokens reais emitidos pelo /auth/login.
@SpringBootTest(properties = {
        "rkfbank.admin.cpf=" + SegurancaIntegracaoTest.ADMIN_CPF,
        "rkfbank.admin.senha=" + SegurancaIntegracaoTest.ADMIN_SENHA
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaIntegracaoTest {

    static final String ADMIN_CPF = "11144477735";
    static final String ADMIN_SENHA = "senha-do-admin-teste";
    private static final String SENHA = "senha-forte-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ViaCEP e externo: fora do teste de seguranca.
    @MockitoBean
    private ClienteViaCep clienteViaCep;

    @BeforeEach
    void setUp() {
        when(clienteViaCep.buscarPorCep(anyString())).thenReturn(new ResponseViaCepDto(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308", false));
    }

    // ---------- 401 ----------

    @Test
    void semTokenDa401NoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/contas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Autenticacao necessaria: envie o token no header Authorization (Bearer)"))
                .andExpect(jsonPath("$.path").value("/contas"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    void tokenExpiradoDa401() throws Exception {
        Instant duasHorasAtras = Instant.now().minus(2, ChronoUnit.HOURS);
        // Assinado com a chave certa, mas venceu ha 1 h (bem alem dos 60 s de tolerancia de relogio).
        String expirado = assinar(jwtEncoder, JwtClaimsSet.builder()
                .issuer(TokenService.EMISSOR)
                .subject(UUID.randomUUID().toString())
                .issuedAt(duasHorasAtras)
                .expiresAt(duasHorasAtras.plus(1, ChronoUnit.HOURS))
                .claim(TokenService.CLAIM_PERFIL, "ADMIN")
                .build());

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token invalido ou expirado; faca login novamente"));
    }

    @Test
    void tokenAssinadoComOutraChaveDa401() throws Exception {
        JwtEncoder encoderDeFora = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "uma-chave-que-a-api-nao-conhece-com-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String forjado = assinar(encoderDeFora, JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim(TokenService.CLAIM_PERFIL, "ADMIN")
                .build());

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token invalido ou expirado; faca login novamente"));
    }

    @Test
    void tokenSemAssinaturaAlgNoneDa401() throws Exception {
        // Ataque classico: header "alg":"none" e assinatura vazia. O decoder so aceita HS256.
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"sub\":\"" + UUID.randomUUID() + "\",\"perfil\":\"ADMIN\",\"exp\":"
                + Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + header + "." + payload + "."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilAdulteradoNoPayloadDa401() throws Exception {
        String cpf = cpfAleatorio();
        cadastrar(cpf);
        String[] partes = login(cpf, SENHA).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"CLIENTE\"", "\"ADMIN\"");
        String adulterado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metadadoDoRecursoProtegidoEPublicoESoDizComoMandarOToken() throws Exception {
        // Publicado pelo Spring Security 7 (RFC 9728); achado listando a cadeia de filtros. Nao pode vazar nada alem disso.
        mockMvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"))
                .andExpect(jsonPath("$", not(hasKey("jwks_uri"))))
                .andExpect(jsonPath("$", not(hasKey("authorization_servers"))));
    }

    @Test
    void tokenQueNaoEJwtDa401() throws Exception {
        mockMvc.perform(get("/contas").header("Authorization", "Bearer isso-nao-e-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ---------- cadastro e login ----------

    @Test
    void cadastroEPublicoNaoDevolveSenhaEGuardaSoOHashBcrypt() throws Exception {
        String cpf = cpfAleatorio();

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastroJson(cpf, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("senhaHash"))));

        Usuario usuario = usuarioRepository.findByLogin(cpf).orElseThrow();
        assertTrue(usuario.getSenhaHash().startsWith("$2a$10$"), "hash BCrypt com custo 10");
        assertNotEquals(SENHA, usuario.getSenhaHash());
        assertTrue(passwordEncoder.matches(SENHA, usuario.getSenhaHash()));
    }

    @Test
    void cadastroSemSenhaDa400() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastroJson(cpfAleatorio(), "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'senha')]").exists());
    }

    @Test
    void loginOkDevolveTokenComExpiracaoEClienteId() throws Exception {
        String cpf = cpfAleatorio();
        String clienteId = cadastrar(cpf);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(900))
                .andExpect(jsonPath("$.clienteId").value(clienteId))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
    }

    @Test
    void senhaErradaDa401() throws Exception {
        String cpf = cpfAleatorio();
        cadastrar(cpf);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("CPF ou senha invalidos"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void cpfSemCadastroDaAMesmaRespostaDaSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpfAleatorio(), SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("CPF ou senha invalidos"));
    }

    // ---------- regra de dono (403) ----------

    @Test
    void contaDeOutroClienteDa403EmTodasAsOperacoes() throws Exception {
        String cpfAna = cpfAleatorio();
        String clienteAna = cadastrar(cpfAna);
        String tokenAna = login(cpfAna, SENHA);
        String contaAna = abrirConta(clienteAna, tokenAna);

        String cpfBeto = cpfAleatorio();
        String clienteBeto = cadastrar(cpfBeto);
        String tokenBeto = login(cpfBeto, SENHA);
        String contaBeto = abrirConta(clienteBeto, tokenBeto);

        // A dona movimenta a propria conta.
        mockMvc.perform(post("/contas/" + contaAna + "/deposito").header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 100.00, \"descricao\": \"salario\"}"))
                .andExpect(status().isNoContent());

        // Beto, com token valido, tenta mexer na conta da Ana.
        mockMvc.perform(post("/contas/" + contaAna + "/saque").header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 50.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Conta nao pertence ao usuario logado"))
                .andExpect(jsonPath("$.path").value("/contas/" + contaAna + "/saque"));

        mockMvc.perform(post("/contas/" + contaAna + "/deposito").header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 1.00}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/contas/" + contaAna + "/lancamentos").header("Authorization", "Bearer " + tokenBeto))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/contas/transferencia").header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\": \"" + contaAna + "\", \"contaDestinoId\": \"" + contaBeto + "\", \"valor\": 10.00}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/contas/clientes/" + clienteAna).header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON).content(contaJson()))
                .andExpect(status().isForbidden());

        // Transferir PARA a conta de outro cliente e permitido: so a origem precisa ser sua.
        mockMvc.perform(post("/contas/transferencia").header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\": \"" + contaAna + "\", \"contaDestinoId\": \"" + contaBeto + "\", \"valor\": 10.00}"))
                .andExpect(status().isNoContent());

        // Cada cliente so enxerga as proprias contas em GET /contas.
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + tokenBeto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(contaBeto));
    }

    // ---------- perfil (403 de rota) ----------

    @Test
    void clienteNaoListaClientes() throws Exception {
        String cpf = cpfAleatorio();
        cadastrar(cpf);

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + login(cpf, SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado para o seu perfil"))
                .andExpect(jsonPath("$.path").value("/clientes"));
    }

    @Test
    void adminCriadoPeloEnvListaClientesEContas() throws Exception {
        String tokenAdmin = login(ADMIN_CPF, ADMIN_SENHA);

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/contas").param("paginado", "true").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------- apoio ----------

    private String cadastrar(String cpf) throws Exception {
        String corpo = mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(cadastroJson(cpf, SENHA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("id").asString();
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }

    private String abrirConta(String clienteId, String token) throws Exception {
        String corpo = mockMvc.perform(post("/contas/clientes/" + clienteId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(contaJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode conta = jsonMapper.readTree(corpo);
        return conta.get("id").asString();
    }

    private static String assinar(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static String cadastroJson(String cpf, String senha) {
        return """
                {"nomeCompleto": "Cliente Teste", "cpf": "%s", "email": "%s@rkfbank.com", "telefone": "11999998888",
                 "cep": "01001000", "numero": "10", "senha": "%s"}
                """.formatted(cpf, cpf, senha);
    }

    private static String loginJson(String cpf, String senha) {
        return "{\"cpf\": \"" + cpf + "\", \"senha\": \"" + senha + "\"}";
    }

    private static String contaJson() {
        String numero = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        return "{\"agencia\": \"0001\", \"numeroConta\": \"" + numero + "\", \"tipoContaEnum\": \"CORRENTE\"}";
    }

    // O H2 e compartilhado entre os testes: cada um usa um CPF novo (valido, com os 2 digitos verificadores).
    private static String cpfAleatorio() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
            d[9] = digito(d, 9);
            d[10] = digito(d, 10);
        } while (ADMIN_CPF.equals(texto(d)));
        return texto(d);
    }

    private static int digito(int[] d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += d[i] * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static String texto(int[] d) {
        StringBuilder sb = new StringBuilder();
        for (int n : d) {
            sb.append(n);
        }
        return sb.toString();
    }
}
