package com.br.rkfbank.clientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.contas.Conta;
import com.br.rkfbank.contas.TipoContaEnum;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
// Perfil test explicito: roda igual no IDE e no Maven (H2, sem seed, sem o .env de quem roda).
@ActiveProfiles("test")
// H2 proprio dos testes de repository (ver UsuarioRepositoryTest).
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:repositorios;MODE=MySQL")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void deveSalvarClienteGerandoIdEPersistindoEnderecoEmCascata() {
        Cliente salvo = clienteRepository.saveAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));
        entityManager.clear();

        Cliente encontrado = clienteRepository.findById(salvo.getId()).orElseThrow();

        assertNotNull(encontrado.getId());
        assertEquals("Joao Silva", encontrado.getNomeCompleto());
        assertNotNull(encontrado.getEndereco().getId());
        assertEquals("01001-000", encontrado.getEndereco().getCep());
        assertEquals("Apto 45", encontrado.getEndereco().getComplemento());
    }

    @Test
    void deveRetornarTrueQuandoCpfJaExiste() {
        entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        assertTrue(clienteRepository.existsByCpf("52998224725"));
    }

    @Test
    void deveRetornarFalseQuandoCpfNaoExiste() {
        entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        assertFalse(clienteRepository.existsByCpf("11144477735"));
    }

    @Test
    void deveRetornarTrueQuandoEmailJaExiste() {
        entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        assertTrue(clienteRepository.existsByEmail("joao@rkfbank.com"));
    }

    @Test
    void deveRetornarFalseQuandoEmailNaoExiste() {
        entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        assertFalse(clienteRepository.existsByEmail("maria@rkfbank.com"));
    }

    @Test
    void deveViolarUnicidadeAoSalvarCpfDuplicado() {
        clienteRepository.saveAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        Cliente duplicado = novoCliente("52998224725", "outro@rkfbank.com");

        assertThrows(DataIntegrityViolationException.class, () -> clienteRepository.saveAndFlush(duplicado));
    }

    @Test
    void deveViolarUnicidadeAoSalvarEmailDuplicado() {
        clienteRepository.saveAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));

        Cliente duplicado = novoCliente("11144477735", "joao@rkfbank.com");

        assertThrows(DataIntegrityViolationException.class, () -> clienteRepository.saveAndFlush(duplicado));
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarClienteSemEndereco() {
        Cliente semEndereco = novoCliente("52998224725", "joao@rkfbank.com");
        semEndereco.setEndereco(null);

        assertThrows(DataIntegrityViolationException.class, () -> clienteRepository.saveAndFlush(semEndereco));
    }

    @Test
    void deveListarClientesPaginados() {
        entityManager.persist(novoCliente("52998224725", "joao@rkfbank.com"));
        entityManager.persist(novoCliente("11144477735", "maria@rkfbank.com"));
        entityManager.persist(novoCliente("39053344705", "jose@rkfbank.com"));
        entityManager.flush();

        Page<Cliente> pagina = clienteRepository.findAll(PageRequest.of(0, 2));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(2, pagina.getContent().size());
    }

    @Test
    void deveSalvarContasEmCascataAoSalvarCliente() {
        Cliente cliente = novoCliente("52998224725", "joao@rkfbank.com");
        cliente.getContas().add(novaConta(cliente, "12345678"));

        Cliente salvo = clienteRepository.saveAndFlush(cliente);
        entityManager.clear();

        Cliente encontrado = clienteRepository.findById(salvo.getId()).orElseThrow();

        assertEquals(1, encontrado.getContas().size());
        assertNotNull(encontrado.getContas().getFirst().getId());
        assertEquals("12345678", encontrado.getContas().getFirst().getNumeroConta());
    }

    // Regra de banco (mudou em 20/10/2026): conta nunca e apagada por tabela; e encerrada com ativa = false.
    // Antes, orphanRemoval apagava a conta ao tira-la da lista do cliente, e o extrato perdia a referencia.
    @Test
    void tirarAContaDaListaDoClienteNaoApagaAConta() {
        Cliente cliente = novoCliente("52998224725", "joao@rkfbank.com");
        cliente.getContas().add(novaConta(cliente, "12345678"));
        Cliente salvo = clienteRepository.saveAndFlush(cliente);
        entityManager.clear();

        Cliente encontrado = clienteRepository.findById(salvo.getId()).orElseThrow();
        encontrado.getContas().clear();
        clienteRepository.saveAndFlush(encontrado);
        entityManager.clear();

        assertEquals(1L, contar("Conta"));
    }

    @Test
    void naoExcluiClienteQueAindaTemConta() {
        Cliente cliente = novoCliente("52998224725", "joao@rkfbank.com");
        cliente.getContas().add(novaConta(cliente, "12345678"));
        Cliente salvo = clienteRepository.saveAndFlush(cliente);
        entityManager.clear();

        // Sem cascata de REMOVE nas contas, a FK fk_conta_cliente barra a exclusao.
        assertThrows(DataIntegrityViolationException.class, () -> {
            clienteRepository.deleteById(salvo.getId());
            clienteRepository.flush();
        });
    }

    @Test
    void excluirClienteSemContaLevaOEnderecoJunto() {
        Cliente salvo = clienteRepository.saveAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));
        entityManager.clear();

        clienteRepository.deleteById(salvo.getId());
        clienteRepository.flush();

        assertFalse(clienteRepository.existsById(salvo.getId()));
        assertEquals(0L, contar("Endereco"));
    }

    private long contar(String entidade) {
        return entityManager.getEntityManager()
                .createQuery("select count(e) from " + entidade + " e", Long.class)
                .getSingleResult();
    }

    private Conta novaConta(Cliente dono, String numeroConta) {
        Conta conta = new Conta();
        conta.setNumeroConta(numeroConta);
        conta.setAgencia("0001");
        conta.setTipoConta(TipoContaEnum.CORRENTE);
        conta.setSaldo(BigDecimal.ZERO);
        conta.setAtiva(true);
        conta.setCliente(dono);
        return conta;
    }

    private Cliente novoCliente(String cpf, String email) {
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");
        endereco.setComplemento("Apto 45");

        Cliente cliente = new Cliente();
        cliente.setNomeCompleto("Joao Silva");
        cliente.setCpf(cpf);
        cliente.setEmail(email);
        cliente.setTelefone("11999999999");
        cliente.setEndereco(endereco);
        return cliente;
    }
}
