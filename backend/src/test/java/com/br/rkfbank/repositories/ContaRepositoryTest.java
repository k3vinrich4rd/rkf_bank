package com.br.rkfbank.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
// Perfil test explicito: roda igual no IDE e no Maven (H2, sem seed, sem o .env de quem roda).
@ActiveProfiles("test")
// H2 proprio dos testes de repository (ver UsuarioRepositoryTest).
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:repositorios;MODE=MySQL")
class ContaRepositoryTest {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = entityManager.persistAndFlush(novoCliente());
    }

    @Test
    void deveSalvarContaVinculadaAoCliente() {
        Conta salva = contaRepository.saveAndFlush(novaConta("12345678"));
        entityManager.clear();

        Conta encontrada = contaRepository.findById(salva.getId()).orElseThrow();

        assertNotNull(encontrada.getId());
        assertEquals("12345678", encontrada.getNumeroConta());
        assertEquals("0001", encontrada.getAgencia());
        assertEquals(TipoContaEnum.CORRENTE, encontrada.getTipoConta());
        assertEquals(0, new BigDecimal("150.75").compareTo(encontrada.getSaldo()));
        assertTrue(encontrada.isAtiva());
        assertEquals(cliente.getId(), encontrada.getCliente().getId());
    }

    @Test
    void devePersistirTipoContaComoTexto() {
        Conta salva = contaRepository.saveAndFlush(novaConta("12345678"));

        Object tipoPersistido = entityManager.getEntityManager()
                .createNativeQuery("select tipo_conta_enum from tb_conta where id = :id")
                .setParameter("id", salva.getId())
                .getSingleResult();

        assertEquals("CORRENTE", tipoPersistido);
    }

    @Test
    void deveRetornarTrueQuandoNumeroContaJaExiste() {
        entityManager.persistAndFlush(novaConta("12345678"));

        assertTrue(contaRepository.existsByNumeroConta("12345678"));
    }

    @Test
    void deveRetornarFalseQuandoNumeroContaNaoExiste() {
        entityManager.persistAndFlush(novaConta("12345678"));

        assertFalse(contaRepository.existsByNumeroConta("87654321"));
    }

    @Test
    void deveViolarUnicidadeAoSalvarNumeroContaDuplicado() {
        contaRepository.saveAndFlush(novaConta("12345678"));

        Conta duplicada = novaConta("12345678");

        assertThrows(DataIntegrityViolationException.class, () -> contaRepository.saveAndFlush(duplicada));
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarContaSemCliente() {
        Conta semCliente = novaConta("12345678");
        semCliente.setCliente(null);

        assertThrows(DataIntegrityViolationException.class, () -> contaRepository.saveAndFlush(semCliente));
    }

    @Test
    void deveIncrementarVersaoAoAtualizarConta() {
        Conta conta = contaRepository.saveAndFlush(novaConta("12345678"));
        Long versaoInicial = (Long) ReflectionTestUtils.getField(conta, "versao");

        conta.setSaldo(new BigDecimal("200.00"));
        contaRepository.saveAndFlush(conta);

        Long versaoAtualizada = (Long) ReflectionTestUtils.getField(conta, "versao");
        assertNotNull(versaoInicial);
        assertEquals(versaoInicial + 1, versaoAtualizada);
    }

    @Test
    void deveRejeitarAtualizacaoComVersaoDesatualizada() {
        Conta salva = contaRepository.saveAndFlush(novaConta("12345678"));
        entityManager.clear();

        // Copia obsoleta: carregada antes da atualizacao concorrente e desanexada do contexto.
        Conta obsoleta = contaRepository.findById(salva.getId()).orElseThrow();
        entityManager.detach(obsoleta);

        Conta atual = contaRepository.findById(salva.getId()).orElseThrow();
        atual.setSaldo(new BigDecimal("300.00"));
        contaRepository.saveAndFlush(atual);

        obsoleta.setSaldo(new BigDecimal("999.00"));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> contaRepository.saveAndFlush(obsoleta));
    }

    @Test
    void devePersistirContaPoupancaInativa() {
        Conta poupanca = novaConta("12345678");
        poupanca.setTipoConta(TipoContaEnum.POUPANCA);
        poupanca.setAtiva(false);
        Conta salva = contaRepository.saveAndFlush(poupanca);
        entityManager.clear();

        Conta encontrada = contaRepository.findById(salva.getId()).orElseThrow();

        assertEquals(TipoContaEnum.POUPANCA, encontrada.getTipoConta());
        assertFalse(encontrada.isAtiva());
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarContaSemSaldo() {
        Conta semSaldo = novaConta("12345678");
        semSaldo.setSaldo(null);

        assertThrows(DataIntegrityViolationException.class, () -> contaRepository.saveAndFlush(semSaldo));
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarContaSemTipo() {
        Conta semTipo = novaConta("12345678");
        semTipo.setTipoConta(null);

        assertThrows(DataIntegrityViolationException.class, () -> contaRepository.saveAndFlush(semTipo));
    }

    @Test
    void deveListarContasPaginadas() {
        entityManager.persist(novaConta("11111111"));
        entityManager.persist(novaConta("22222222"));
        entityManager.persist(novaConta("33333333"));
        entityManager.flush();

        Page<Conta> pagina = contaRepository.findAll(PageRequest.of(1, 2));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(1, pagina.getContent().size());
        assertTrue(pagina.isLast());
    }

    private Conta novaConta(String numeroConta) {
        Conta conta = new Conta();
        conta.setNumeroConta(numeroConta);
        conta.setAgencia("0001");
        conta.setTipoConta(TipoContaEnum.CORRENTE);
        conta.setSaldo(new BigDecimal("150.75"));
        conta.setAtiva(true);
        conta.setCliente(cliente);
        return conta;
    }

    private Cliente novoCliente() {
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");

        Cliente novo = new Cliente();
        novo.setNomeCompleto("Joao Silva");
        novo.setCpf("52998224725");
        novo.setEmail("joao@rkfbank.com");
        novo.setTelefone("11999999999");
        novo.setEndereco(endereco);
        return novo;
    }
}
