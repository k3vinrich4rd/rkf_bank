package com.br.rkfbank.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.entities.Lancamento;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
// Perfil test explicito: roda igual no IDE e no Maven (H2, sem seed, sem o .env de quem roda).
@ActiveProfiles("test")
class LancamentoRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-10T12:00:00Z");

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void devePreencherDataHoraAutomaticamenteAoSalvar() {
        Lancamento lancamento = novoLancamento(TipoLancamentoEnum.DEPOSITO, null, UUID.randomUUID());

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);

        assertNotNull(salvo.getId());
        assertNotNull(salvo.getDataHora());
    }

    @Test
    void deveBuscarExtratoComLancamentosDeOrigemEDestinoOrdenadosDoMaisNovoParaOMaisAntigo() {
        UUID contaId = UUID.randomUUID();
        UUID outraConta = UUID.randomUUID();

        Lancamento deposito = persistirEm(novoLancamento(TipoLancamentoEnum.DEPOSITO, null, contaId), 0);
        Lancamento saque = persistirEm(novoLancamento(TipoLancamentoEnum.SAQUE, contaId, null), 2);
        Lancamento transferenciaEnviada =
                persistirEm(novoLancamento(TipoLancamentoEnum.TRANSFERENCIA, contaId, outraConta), 1);
        Lancamento transferenciaRecebida =
                persistirEm(novoLancamento(TipoLancamentoEnum.TRANSFERENCIA, outraConta, contaId), 3);
        entityManager.clear();

        List<Lancamento> extrato =
                lancamentoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId);

        assertEquals(
                List.of(transferenciaRecebida.getId(), saque.getId(), transferenciaEnviada.getId(), deposito.getId()),
                extrato.stream().map(Lancamento::getId).toList());
    }

    @Test
    void naoDeveIncluirLancamentosDeOutrasContasNoExtrato() {
        UUID contaId = UUID.randomUUID();
        UUID contaA = UUID.randomUUID();
        UUID contaB = UUID.randomUUID();

        Lancamento daConta = persistirEm(novoLancamento(TipoLancamentoEnum.DEPOSITO, null, contaId), 0);
        persistirEm(novoLancamento(TipoLancamentoEnum.DEPOSITO, null, contaA), 1);
        persistirEm(novoLancamento(TipoLancamentoEnum.TRANSFERENCIA, contaA, contaB), 2);
        entityManager.clear();

        List<Lancamento> extrato =
                lancamentoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId);

        assertEquals(1, extrato.size());
        assertEquals(daConta.getId(), extrato.getFirst().getId());
    }

    @Test
    void deveRetornarListaVaziaQuandoContaNaoPossuiLancamentos() {
        persistirEm(novoLancamento(TipoLancamentoEnum.DEPOSITO, null, UUID.randomUUID()), 0);

        UUID semMovimento = UUID.randomUUID();
        List<Lancamento> extrato =
                lancamentoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(semMovimento, semMovimento);

        assertTrue(extrato.isEmpty());
    }

    @Test
    void devePersistirCamposDoLancamento() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        Lancamento salvo = persistirEm(novoLancamento(TipoLancamentoEnum.TRANSFERENCIA, origem, destino), 0);
        entityManager.clear();

        Lancamento encontrado = lancamentoRepository.findById(salvo.getId()).orElseThrow();

        assertEquals(TipoLancamentoEnum.TRANSFERENCIA, encontrado.getTipoLancamentoEnum());
        assertEquals(0, new BigDecimal("25.50").compareTo(encontrado.getValor()));
        assertEquals(origem, encontrado.getContaOrigemId());
        assertEquals(destino, encontrado.getContaDestinoId());
        assertEquals("Lancamento de teste", encontrado.getDescricao());
        assertEquals(BASE, encontrado.getDataHora());
    }

    @Test
    void deveSobrescreverDataHoraInformadaAoSalvar() {
        Lancamento lancamento = novoLancamento(TipoLancamentoEnum.DEPOSITO, null, UUID.randomUUID());
        ReflectionTestUtils.setField(lancamento, "dataHora", BASE);
        Instant antes = Instant.now();

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);

        assertFalse(salvo.getDataHora().isBefore(antes));
    }

    @Test
    void devePersistirTipoLancamentoComoTexto() {
        Lancamento salvo = persistirEm(novoLancamento(TipoLancamentoEnum.SAQUE, UUID.randomUUID(), null), 0);

        Object tipoPersistido = entityManager.getEntityManager()
                .createNativeQuery("select tipo_lancamento_enum from tb_lancamento where id = :id")
                .setParameter("id", salvo.getId())
                .getSingleResult();

        assertEquals("SAQUE", tipoPersistido);
    }

    @Test
    void naoDeveDuplicarLancamentoNoExtratoQuandoContaEhOrigemEDestino() {
        UUID contaId = UUID.randomUUID();
        persistirEm(novoLancamento(TipoLancamentoEnum.TRANSFERENCIA, contaId, contaId), 0);
        entityManager.clear();

        List<Lancamento> extrato =
                lancamentoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId);

        assertEquals(1, extrato.size());
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarLancamentoSemValor() {
        Lancamento semValor = novoLancamento(TipoLancamentoEnum.DEPOSITO, null, UUID.randomUUID());
        semValor.setValor(null);

        assertThrows(DataIntegrityViolationException.class, () -> lancamentoRepository.saveAndFlush(semValor));
    }

    @Test
    void deveViolarObrigatoriedadeAoSalvarLancamentoSemTipo() {
        Lancamento semTipo = novoLancamento(null, null, UUID.randomUUID());

        assertThrows(DataIntegrityViolationException.class, () -> lancamentoRepository.saveAndFlush(semTipo));
    }

    // O @PrePersist sobrescreve dataHora com Instant.now(); fixamos o horario depois do insert
    // para que a ordenacao do extrato seja deterministica.
    private Lancamento persistirEm(Lancamento lancamento, long minutosAposBase) {
        entityManager.persistAndFlush(lancamento);
        ReflectionTestUtils.setField(lancamento, "dataHora", BASE.plusSeconds(minutosAposBase * 60));
        entityManager.flush();
        return lancamento;
    }

    private Lancamento novoLancamento(TipoLancamentoEnum tipo, UUID contaOrigemId, UUID contaDestinoId) {
        Lancamento lancamento = new Lancamento();
        lancamento.setTipoLancamentoEnum(tipo);
        lancamento.setValor(new BigDecimal("25.50"));
        lancamento.setContaOrigemId(contaOrigemId);
        lancamento.setContaDestinoId(contaDestinoId);
        lancamento.setDescricao("Lancamento de teste");
        return lancamento;
    }
}
