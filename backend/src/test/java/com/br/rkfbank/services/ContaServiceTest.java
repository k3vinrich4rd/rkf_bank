package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.entities.Lancamento;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.LancamentoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private LancamentoRepository lancamentoRepository;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ContaService contaService;

    @Test
    void deveAbrirContaComSaldoInicialZero() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);

        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        when(clienteService.buscar(clienteId)).thenReturn(cliente);
        when(contaRepository.existsByNumeroConta("12345678")).thenReturn(false);
        when(contaRepository.saveAndFlush(any(Conta.class))).thenAnswer(invocation -> {
            Conta conta = invocation.getArgument(0);
            ReflectionTestUtils.setField(conta, "id", UUID.randomUUID());
            return conta;
        });

        ContaResponseDto response = contaService.abrir(clienteId, request);

        assertNotNull(response.id());
        assertEquals(new BigDecimal("0.00"), response.saldo());
        assertEquals(TipoContaEnum.CORRENTE, response.tipoContaEnum());
    }

    @Test
    void deveImpedirAberturaComNumeroContaDuplicado() {
        UUID clienteId = UUID.randomUUID();
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        when(clienteService.buscar(clienteId)).thenReturn(new Cliente());
        when(contaRepository.existsByNumeroConta("12345678")).thenReturn(true);

        assertThrows(NegocioException.class, () -> contaService.abrir(clienteId, request));
    }

    @Test
    void deveConverterConflitoDeNumeroContaNoInsertEm409() {
        UUID clienteId = UUID.randomUUID();
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        when(clienteService.buscar(clienteId)).thenReturn(new Cliente());
        when(contaRepository.existsByNumeroConta("12345678")).thenReturn(false);
        when(contaRepository.saveAndFlush(any(Conta.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate account number"));

        assertThrows(NegocioException.class, () -> contaService.abrir(clienteId, request));
    }

    @Test
    void deveDepositarERegistrarLancamento() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        contaService.depositar(contaId, new DepositarRequestDto(new BigDecimal("5.00"), "Deposito"));

        assertEquals(new BigDecimal("15.00"), conta.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarDepositoComValorInvalido() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.depositar(contaId, new DepositarRequestDto(BigDecimal.ZERO, "Deposito")));
    }

    @Test
    void deveLancar404QuandoContaNaoExisteNoDeposito() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThrows(ContaNaoEncontradaException.class,
                () -> contaService.depositar(contaId, new DepositarRequestDto(new BigDecimal("1.00"), "Deposito")));
    }

    @Test
    void deveSacarERegistrarLancamento() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("50.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("20.00"), "Saque"));

        assertEquals(new BigDecimal("30.00"), conta.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarSaqueSemSaldo() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("15.00"), "Saque")));

        verify(lancamentoRepository, never()).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarSaqueComContaInativa() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), false);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("1.00"), "Saque")));
    }

    @Test
    void deveTransferirEntreContasERegistrarLancamento() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        Conta origem = novaConta(origemId, new BigDecimal("100.00"), true);
        Conta destino = novaConta(destinoId, new BigDecimal("20.00"), true);

        when(contaRepository.findById(eq(origemId))).thenReturn(Optional.of(origem));
        when(contaRepository.findById(eq(destinoId))).thenReturn(Optional.of(destino));

        contaService.transferir(new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "Transferencia"));

        assertEquals(new BigDecimal("70.00"), origem.getSaldo());
        assertEquals(new BigDecimal("50.00"), destino.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarTransferenciaParaMesmaConta() {
        UUID contaId = UUID.randomUUID();

        assertThrows(NaoProcessavelException.class,
                () -> contaService.transferir(new TransferirRequestDto(contaId, contaId, new BigDecimal("10.00"), "x")));
    }

    @Test
    void deveRecusarTransferenciaSemSaldo() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        Conta origem = novaConta(origemId, new BigDecimal("5.00"), true);
        Conta destino = novaConta(destinoId, new BigDecimal("20.00"), true);

        when(contaRepository.findById(eq(origemId))).thenReturn(Optional.of(origem));
        when(contaRepository.findById(eq(destinoId))).thenReturn(Optional.of(destino));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.transferir(new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "x")));
    }

    @Test
    void deveRecusarTransferenciaComValorNulo() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        assertThrows(NaoProcessavelException.class,
                () -> contaService.transferir(new TransferirRequestDto(origemId, destinoId, null, "x")));
    }

    @Test
    void deveListarLancamentosDaConta() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);

        Lancamento lancamento = new Lancamento();
        ReflectionTestUtils.setField(lancamento, "id", UUID.randomUUID());
        lancamento.setTipoLancamentoEnum(TipoLancamentoEnum.DEPOSITO);
        lancamento.setValor(new BigDecimal("10.00"));
        ReflectionTestUtils.setField(lancamento, "dataHora", Instant.now());

        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId))
                .thenReturn(List.of(lancamento));

        List<LancamentoResponseDto> response = contaService.listarLancamentos(contaId);

        assertEquals(1, response.size());
        assertEquals(TipoLancamentoEnum.DEPOSITO, response.getFirst().tipoLancamento());
    }

    @Test
    void deveListarContasSemPaginacao() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findAll()).thenReturn(List.of(conta));

        List<ContaResponseDto> response = contaService.listarTodas();

        assertEquals(1, response.size());
    }

    @Test
    void deveListarContasPaginado() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(conta)));

        assertEquals(1, contaService.listarPaginado(PageRequest.of(0, 10)).getTotalElements());
    }

    private Conta novaConta(UUID id, BigDecimal saldo, boolean ativa) {
        Conta conta = new Conta();
        ReflectionTestUtils.setField(conta, "id", id);
        conta.setAgencia("0001");
        conta.setNumeroConta("12345678");
        conta.setTipoConta(TipoContaEnum.CORRENTE);
        conta.setSaldo(saldo);
        conta.setAtiva(ativa);

        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        conta.setCliente(cliente);
        return conta;
    }
}
