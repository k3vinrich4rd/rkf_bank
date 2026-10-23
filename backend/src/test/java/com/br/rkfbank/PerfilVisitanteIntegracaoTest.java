package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.services.TokenService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

// O perfil visitante de verdade (application-visitante.properties), num H2 proprio. Prova as garantias do
// VISITANTES.md: entram so a Ana e o Bruno, nenhuma conta da equipe nem admin existe, e nem o segredo de teste
// nem o do .env assinam token aceito (a chave e sorteada na subida).
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:perfil-visitante;DB_CLOSE_DELAY=-1",
        // Mesmo que alguem tenha esses valores no ambiente, o perfil visitante nao pode usa-los.
        "ADMIN_CPF=11144477735", "ADMIN_SENHA=senha-de-admin-do-ambiente", "EQUIPE_SENHA=senha-da-equipe-do-ambiente",
        "JWT_SEGREDO=" + PerfilVisitanteIntegracaoTest.SEGREDO_DO_AMBIENTE
})
@AutoConfigureMockMvc
@ActiveProfiles("visitante")
class PerfilVisitanteIntegracaoTest {

    static final String SEGREDO_DO_AMBIENTE = "segredo-que-estaria-no-env-com-mais-de-32-bytes";
    private static final String SENHA_VISITANTE = "Visitante@2026";
    private static final String CONTA_ANA = "a0000000-0000-4000-8000-000000000114";
    private static final String CONTA_BRUNO = "a0000000-0000-4000-8000-000000000124";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void anaEBrunoEntramComASenhaPublicaEVeemSoAPropriaConta() throws Exception {
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login("246.813.579-28", SENHA_VISITANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(CONTA_ANA))
                .andExpect(jsonPath("$[0].numeroConta").value("900001"));

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login("97531864282", SENHA_VISITANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONTA_BRUNO));
    }

    @Test
    void naoExisteContaDaEquipeNemAdmin() throws Exception {
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_cliente WHERE cpf IN ('12345678909', '98765432100')", Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_usuario WHERE perfil = 'ADMIN'", Long.class));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"11144477735\",\"senha\":\"senha-de-admin-do-ambiente\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOSegredoDoAmbienteNaoVale() throws Exception {
        // Se o visitante usasse o JWT_SEGREDO do .env (ou um segredo escrito no Git), este token forjado de ADMIN passaria.
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SEGREDO_DO_AMBIENTE.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String forjado = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(TokenService.EMISSOR)
                        .subject(UUID.randomUUID().toString())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .claim(TokenService.CLAIM_PERFIL, "ADMIN")
                        .build())).getTokenValue();

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void visitanteTransfereComIdempotencyKey() throws Exception {
        mockMvc.perform(post("/transferencias").header("Authorization", "Bearer " + login("24681357928", SENHA_VISITANTE))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":\"" + CONTA_ANA + "\",\"contaDestinoId\":\"" + CONTA_BRUNO + "\",\"valor\":10.00}"))
                .andExpect(status().isNoContent());
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEmSegundos").value(3600))
                .andReturn().getResponse().getContentAsString();
        return corpo.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}
