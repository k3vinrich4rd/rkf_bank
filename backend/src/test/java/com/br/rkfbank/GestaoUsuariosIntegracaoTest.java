package com.br.rkfbank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// O roteiro de gestao de usuarios do guia da equipe, de ponta a ponta: o admin promove o Felipe, rebaixa, desativa
// e reativa o Renan, e cada mudanca derruba na hora o token emitido antes dela (Spring Security conferindo
// tb_usuario a cada requisicao). DirtiesContext: cada teste comeca com o banco e os perfis do seed intactos.
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed/equipe",
        // Sem DB_CLOSE_DELAY=-1: o H2 some quando o contexto fecha, e o proximo teste recebe banco novo.
        "spring.datasource.url=jdbc:h2:mem:gestao-usuarios",
        "rkfbank.equipe.senha=" + GestaoUsuariosIntegracaoTest.SENHA_EQUIPE,
        "rkfbank.admin.cpf=" + GestaoUsuariosIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + GestaoUsuariosIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GestaoUsuariosIntegracaoTest {

    static final String SENHA_EQUIPE = "senha-da-equipe-so-no-teste";
    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";
    private static final String CPF_FELIPE = "12345678909";
    private static final String CPF_RENAN = "98765432100";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void adminListaUsuariosComCpfMascaradoESemSenha() throws Exception {
        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + login(CPF_ADMIN, SENHA_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].perfil").value("ADMIN"))
                .andExpect(jsonPath("$[0].cpf").value("***.792.468-**"))
                .andExpect(jsonPath("$[1].nome").value("Felipe Santana"))
                .andExpect(jsonPath("$[1].ativo").value(true))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }

    @Test
    void clienteNaoEntraNaGestaoDeUsuarios() throws Exception {
        mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + login(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para o seu perfil"));
    }

    @Test
    void promoverDerrubaOTokenAntigoEONovoLoginJaVemComoAdmin() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);
        String tokenAntigoDoFelipe = login(CPF_FELIPE, SENHA_EQUIPE);

        alterar(tokenAdmin, idDoUsuario(tokenAdmin, CPF_FELIPE), "perfil", "{\"perfil\":\"ADMIN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        // O token antigo dizia CLIENTE; o banco agora diz ADMIN: 401 e login de novo.
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + tokenAntigoDoFelipe))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Seu acesso foi alterado ou desativado; faca login novamente"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                // Continua cliente: o admin promovido segue dono da propria conta.
                .andExpect(jsonPath("$.clienteId").value("5eed0000-0000-4000-8000-000000000012"));
        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + login(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isOk());
    }

    @Test
    void rebaixarTiraOAcessoDeAdminNaHora() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);
        String felipeId = idDoUsuario(tokenAdmin, CPF_FELIPE);
        alterar(tokenAdmin, felipeId, "perfil", "{\"perfil\":\"ADMIN\"}").andExpect(status().isOk());
        String tokenFelipeAdmin = login(CPF_FELIPE, SENHA_EQUIPE);

        alterar(tokenAdmin, felipeId, "perfil", "{\"perfil\":\"CLIENTE\"}").andExpect(status().isOk());

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + tokenFelipeAdmin))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + login(CPF_FELIPE, SENHA_EQUIPE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void desativarDerrubaTokenELoginEReativarDevolve() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);
        String renanId = idDoUsuario(tokenAdmin, CPF_RENAN);
        String tokenRenan = login(CPF_RENAN, SENHA_EQUIPE);

        alterar(tokenAdmin, renanId, "acesso", "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + tokenRenan))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(CPF_RENAN, SENHA_EQUIPE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("CPF ou senha invalidos"));

        alterar(tokenAdmin, renanId, "acesso", "{\"ativo\":true}").andExpect(status().isOk());

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login(CPF_RENAN, SENHA_EQUIPE)))
                .andExpect(status().isOk());
    }

    @Test
    void trocarAPropriaSenhaDerrubaOTokenAntigoESoANovaEntra() throws Exception {
        String tokenAntigo = login(CPF_RENAN, SENHA_EQUIPE);

        mockMvc.perform(patch("/auth/senha").header("Authorization", "Bearer " + tokenAntigo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"" + SENHA_EQUIPE + "\",\"novaSenha\":\"senha-nova-do-renan\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + tokenAntigo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Seu acesso foi alterado ou desativado; faca login novamente"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(CPF_RENAN, SENHA_EQUIPE)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login(CPF_RENAN, "senha-nova-do-renan")))
                .andExpect(status().isOk());
    }

    @Test
    void senhaAtualErradaNaTrocaE422() throws Exception {
        mockMvc.perform(patch("/auth/senha").header("Authorization", "Bearer " + login(CPF_RENAN, SENHA_EQUIPE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"chute-errado\",\"novaSenha\":\"senha-nova-do-renan\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void adminRedefineASenhaDeQuemEsqueceu() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);
        String tokenAntigoDoFelipe = login(CPF_FELIPE, SENHA_EQUIPE);

        mockMvc.perform(patch("/admin/usuarios/" + idDoUsuario(tokenAdmin, CPF_FELIPE) + "/senha")
                        .header("Authorization", "Bearer " + tokenAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaSenha\":\"senha-temporaria-1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + tokenAntigoDoFelipe))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + login(CPF_FELIPE, "senha-temporaria-1")))
                .andExpect(status().isOk());
    }

    @Test
    void adminNaoMexeEmSiMesmo() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);

        alterar(tokenAdmin, idDoUsuario(tokenAdmin, CPF_ADMIN), "acesso", "{\"ativo\":false}")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Voce nao pode alterar o seu proprio perfil ou acesso; peca a outro ADMIN"));
    }

    @Test
    void usuarioInexistenteE404EPerfilInvalidoE400() throws Exception {
        String tokenAdmin = login(CPF_ADMIN, SENHA_ADMIN);

        alterar(tokenAdmin, "00000000-0000-0000-0000-000000000000", "acesso", "{\"ativo\":false}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario nao encontrado"));
        alterar(tokenAdmin, idDoUsuario(tokenAdmin, CPF_FELIPE), "perfil", "{\"perfil\":\"GERENTE\"}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions alterar(String token, String usuarioId, String oQue, String corpo)
            throws Exception {
        return mockMvc.perform(patch("/admin/usuarios/" + usuarioId + "/" + oQue).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    // O guia ensina a achar o id pelo GET /admin/usuarios; o teste faz o mesmo, pelo trecho visivel do CPF.
    private String idDoUsuario(String tokenAdmin, String cpf) throws Exception {
        String corpo = mockMvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andReturn().getResponse().getContentAsString();
        String meio = cpf.substring(3, 6) + "." + cpf.substring(6, 9);
        for (JsonNode usuario : jsonMapper.readTree(corpo)) {
            if (usuario.get("cpf").asString().contains(meio)) {
                return usuario.get("id").asString();
            }
        }
        throw new IllegalStateException("usuario nao listado");
    }

    private String login(String cpf, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }

    private static String loginJson(String cpf, String senha) {
        return "{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}";
    }
}
