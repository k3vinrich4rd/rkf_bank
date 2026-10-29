package com.br.rkfbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.repositories.ClienteRepository;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

// Quantas consultas SQL cada lista do ADMIN dispara. Com N+1, o numero cresce com a quantidade de linhas; sem ele,
// fica constante. O Hibernate conta os comandos preparados (generate_statistics); a conferencia do token
// (ConversorTokenUsuario) entra na conta, por isso o teste compara 5 clientes com 15 e exige o MESMO numero.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultas-sql;MODE=MySQL",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "rkfbank.admin.cpf=" + ConsultasSqlIntegracaoTest.CPF_ADMIN,
        "rkfbank.admin.senha=" + ConsultasSqlIntegracaoTest.SENHA_ADMIN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConsultasSqlIntegracaoTest {

    static final String CPF_ADMIN = "13579246828";
    static final String SENHA_ADMIN = "senha-do-admin-so-no-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics estatisticas;
    private String tokenAdmin;
    private int criados;

    @BeforeEach
    void setUp() throws Exception {
        estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        tokenAdmin = login();
    }

    @Test
    void listaDeClientesNaoCresceComAQuantidade() throws Exception {
        criarClientes(5);
        long com5 = consultasDe("/clientes");
        criarClientes(10);
        long com15 = consultasDe("/clientes");

        assertThat(com15).as("GET /clientes: 5 clientes = %d consultas, 15 = %d", com5, com15).isEqualTo(com5);
    }

    @Test
    void paginaDeClientesNaoCresceComAQuantidade() throws Exception {
        criarClientes(3);
        long com3 = consultasDe("/clientes?page=0&size=20");
        criarClientes(9);
        long com12 = consultasDe("/clientes?page=0&size=20");

        assertThat(com12).as("GET /clientes paginado: 3 = %d consultas, 12 = %d", com3, com12).isEqualTo(com3);
    }

    @Test
    void listaDeContasDoAdminNaoCresceComAQuantidade() throws Exception {
        criarClientes(5);
        long com5 = consultasDe("/contas");
        criarClientes(10);
        long com15 = consultasDe("/contas");

        assertThat(com15).as("GET /contas (ADMIN): 5 contas = %d consultas, 15 = %d", com5, com15).isEqualTo(com5);
    }

    @Test
    void listaDeUsuariosDoAdminNaoCresceComAQuantidade() throws Exception {
        criarClientes(5);
        long com5 = consultasDe("/admin/usuarios");
        criarClientes(10);
        long com15 = consultasDe("/admin/usuarios");

        assertThat(com15).as("GET /admin/usuarios: 6 usuarios = %d consultas, 16 = %d", com5, com15).isEqualTo(com5);
    }

    private long consultasDe(String rota) throws Exception {
        estatisticas.clear();
        mockMvc.perform(get(rota).header("Authorization", "Bearer " + tokenAdmin)).andExpect(status().isOk());
        return estatisticas.getPrepareStatementCount();
    }

    // Cada cliente com endereco, uma conta e um login, como o cadastro de verdade deixaria.
    private void criarClientes(int quantos) {
        for (int i = 0; i < quantos; i++) {
            int n = ++criados;
            String cpf = String.format("%011d", 90000000000L + n);

            Endereco endereco = new Endereco();
            endereco.setCep("06020-010");
            endereco.setLogradouro("Rua " + n);
            endereco.setBairro("Centro");
            endereco.setCidade("Osasco");
            endereco.setUf("SP");
            endereco.setIbge("3534401");
            endereco.setNumero(String.valueOf(n));

            Cliente cliente = new Cliente();
            cliente.setNomeCompleto("Cliente " + n);
            cliente.setCpf(cpf);
            cliente.setEmail("cliente" + n + "@exemplo.test");
            cliente.setTelefone("11999990000");
            cliente.setEndereco(endereco);
            clienteRepository.save(cliente);

            Conta conta = new Conta();
            conta.setNumeroConta(String.valueOf(700000 + n));
            conta.setAgencia("0001");
            conta.setTipoConta(TipoContaEnum.CORRENTE);
            conta.setSaldo(new BigDecimal("10.00"));
            conta.setAtiva(true);
            conta.setCliente(cliente);
            contaRepository.save(conta);

            Usuario usuario = new Usuario();
            usuario.setLogin(cpf);
            usuario.setSenhaHash("$2a$10$" + "x".repeat(53));
            usuario.setPerfil(PerfilEnum.CLIENTE);
            usuario.setCliente(cliente);
            usuarioRepository.save(usuario);
        }
    }

    private String login() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"" + CPF_ADMIN + "\",\"senha\":\"" + SENHA_ADMIN + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(corpo).get("token").asString();
    }
}
