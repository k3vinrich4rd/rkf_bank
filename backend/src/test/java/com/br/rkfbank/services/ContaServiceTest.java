package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.AcessoNegadoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.LancamentoRepository;
import com.br.rkfbank.security.UsuarioLogado;
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

    // Cliente dono das contas criadas por novaConta(...).
    private static final UUID DONO_ID = UUID.fromString("0b6f3c1e-0000-4000-8000-000000000001");
    private static final UsuarioLogado DONO = new UsuarioLogado(UUID.randomUUID(), DONO_ID, PerfilEnum.CLIENTE);
    private static final UsuarioLogado OUTRO_CLIENTE =
            new UsuarioLogado(UUID.randomUUID(), UUID.fromString("0b6f3c1e-0000-4000-8000-000000000002"), PerfilEnum.CLIENTE);
    private static final UsuarioLogado ADMIN = new UsuarioLogado(UUID.randomUUID(), null, PerfilEnum.ADMIN);

    private static UsuarioLogado donoDe(UUID clienteId) {
        return new UsuarioLogado(UUID.randomUUID(), clienteId, PerfilEnum.CLIENTE);
    }

    @Test
    void deveRecusarAberturaDeContaParaOutroCliente() {
        UUID clienteDeOutro = UUID.randomUUID();
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        assertThrows(AcessoNegadoException.class, () -> contaService.abrir(clienteDeOutro, request, DONO));

        verifyNoInteractions(clienteService);
        verify(contaRepository, never()).saveAndFlush(any(Conta.class));
    }

    @Test
    void adminNaoAbreContaPorNinguem() {
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        assertThrows(AcessoNegadoException.class, () -> contaService.abrir(DONO_ID, request, ADMIN));
    }

    @Test
    void deveRecusarDepositoNaContaDeOutroCliente() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(AcessoNegadoException.class,
                () -> contaService.depositar(contaId, new DepositarRequestDto(new BigDecimal("5.00"), "x"), OUTRO_CLIENTE));

        assertEquals(new BigDecimal("10.00"), conta.getSaldo());
        verifyNoInteractions(lancamentoRepository);
    }

    @Test
    void deveRecusarSaqueNaContaDeOutroClienteAntesDeOlharOSaldo() {
        UUID contaId = UUID.randomUUID();
        // Saldo insuficiente E conta de outro: tem que dar 403, nao 422 (nao revelar o saldo de ninguem).
        Conta conta = novaConta(contaId, new BigDecimal("1.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(AcessoNegadoException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("500.00"), "x"), OUTRO_CLIENTE));
    }

    @Test
    void deveRecusarSaqueEmContaInativaDeOutroClienteCom403() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(novaConta(contaId, new BigDecimal("10.00"), false)));

        assertThrows(AcessoNegadoException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("1.00"), "x"), OUTRO_CLIENTE));
    }

    @Test
    void deveRecusarTransferenciaSaindoDaContaDeOutroCliente() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        Conta origem = novaConta(origemId, new BigDecimal("100.00"), true);
        when(contaRepository.findById(origemId)).thenReturn(Optional.of(origem));

        assertThrows(AcessoNegadoException.class, () -> contaService.transferir(
                new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "x"), OUTRO_CLIENTE));

        assertEquals(new BigDecimal("100.00"), origem.getSaldo());
        verifyNoInteractions(lancamentoRepository);
    }

    @Test
    void devePermitirTransferirParaContaDeOutroCliente() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        Conta origem = novaConta(origemId, new BigDecimal("100.00"), true);
        Conta destino = novaConta(destinoId, new BigDecimal("0.00"), true);
        ReflectionTestUtils.setField(destino.getCliente(), "id", OUTRO_CLIENTE.clienteId());
        when(contaRepository.findById(origemId)).thenReturn(Optional.of(origem));
        when(contaRepository.findById(destinoId)).thenReturn(Optional.of(destino));

        contaService.transferir(new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "x"), DONO);

        assertEquals(new BigDecimal("30.00"), destino.getSaldo());
    }

    @Test
    void deveRecusarExtratoDaContaDeOutroCliente() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(novaConta(contaId, new BigDecimal("10.00"), true)));

        assertThrows(AcessoNegadoException.class, () -> contaService.listarLancamentos(contaId, OUTRO_CLIENTE));

        verifyNoInteractions(lancamentoRepository);
    }

    @Test
    void adminNaoMovimentaContaDeCliente() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(novaConta(contaId, new BigDecimal("10.00"), true)));

        assertThrows(AcessoNegadoException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("1.00"), "x"), ADMIN));
    }

    @Test
    void contaInexistenteContinuaDando404AntesDoDono() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThrows(ContaNaoEncontradaException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("1.00"), "x"), OUTRO_CLIENTE));
    }

    @Test
    void clienteListaSoAsProprias() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findByClienteId(DONO_ID)).thenReturn(List.of(conta));

        List<ContaResponseDto> response = contaService.listarTodas(DONO);

        assertEquals(1, response.size());
        verify(contaRepository, never()).findAll();
    }

    @Test
    void clienteListaSoAsPropriasPaginado() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findByClienteId(DONO_ID, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(conta)));

        assertEquals(1, contaService.listarPaginado(PageRequest.of(0, 10), DONO).getTotalElements());
        verify(contaRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

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

        ContaResponseDto response = contaService.abrir(clienteId, request, donoDe(clienteId));

        assertNotNull(response.id());
        assertEquals(new BigDecimal("0.00"), response.saldo());
        assertEquals(TipoContaEnum.CORRENTE, response.tipoContaEnum());
    }

    @Test
    void semNumeroNoPedidoOBancoGeraOitoDigitosComDigitoVerificadorNaAgenciaPadrao() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        when(clienteService.buscar(clienteId)).thenReturn(cliente);
        when(contaRepository.saveAndFlush(any(Conta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContaResponseDto response = contaService.abrir(clienteId,
                new AbrirContaRequestDto(null, null, TipoContaEnum.POUPANCA), donoDe(clienteId));

        assertEquals(ContaService.AGENCIA_PADRAO, response.agencia());
        String numero = response.numeroConta();
        assertTrue(numero.matches("[1-9]\\d{7}"), numero);
        assertEquals(ContaService.digitoVerificador(numero.substring(0, 7)), Character.getNumericValue(numero.charAt(7)));
    }

    @Test
    void numeroGeradoQueJaExisteESorteadoDeNovo() {
        UUID clienteId = UUID.randomUUID();
        when(clienteService.buscar(clienteId)).thenReturn(new Cliente());
        // Primeiro sorteio colide; o segundo esta livre.
        when(contaRepository.existsByNumeroConta(any())).thenReturn(true, false);
        when(contaRepository.saveAndFlush(any(Conta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contaService.abrir(clienteId, new AbrirContaRequestDto(null, null, TipoContaEnum.CORRENTE), donoDe(clienteId));

        verify(contaRepository, times(3)).existsByNumeroConta(any());
    }

    @Test
    void digitoVerificadorSegueOModulo11() {
        // 1234567: 7*2+6*3+5*4+4*5+3*6+2*7+1*8 = 112; 112 % 11 = 2 -> 11 - 2 = 9.
        assertEquals(9, ContaService.digitoVerificador("1234567"));
        // Resto 0 ou 1 vira 0.
        assertEquals(0, ContaService.digitoVerificador("0000000"));
    }

    @Test
    void deveImpedirAberturaComNumeroContaDuplicado() {
        UUID clienteId = UUID.randomUUID();
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        when(clienteService.buscar(clienteId)).thenReturn(new Cliente());
        when(contaRepository.existsByNumeroConta("12345678")).thenReturn(true);

        assertThrows(NegocioException.class, () -> contaService.abrir(clienteId, request, donoDe(clienteId)));
    }

    @Test
    void deveConverterConflitoDeNumeroContaNoInsertEm409() {
        UUID clienteId = UUID.randomUUID();
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);

        when(clienteService.buscar(clienteId)).thenReturn(new Cliente());
        when(contaRepository.existsByNumeroConta("12345678")).thenReturn(false);
        when(contaRepository.saveAndFlush(any(Conta.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate account number"));

        assertThrows(NegocioException.class, () -> contaService.abrir(clienteId, request, donoDe(clienteId)));
    }

    @Test
    void deveDepositarERegistrarLancamento() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        contaService.depositar(contaId, new DepositarRequestDto(new BigDecimal("5.00"), "Deposito"), DONO);

        assertEquals(new BigDecimal("15.00"), conta.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarDepositoComValorInvalido() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.depositar(contaId, new DepositarRequestDto(BigDecimal.ZERO, "Deposito"), DONO));
    }

    @Test
    void deveLancar404QuandoContaNaoExisteNoDeposito() {
        UUID contaId = UUID.randomUUID();
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        assertThrows(ContaNaoEncontradaException.class,
                () -> contaService.depositar(contaId, new DepositarRequestDto(new BigDecimal("1.00"), "Deposito"), DONO));
    }

    @Test
    void deveSacarERegistrarLancamento() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("50.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("20.00"), "Saque"), DONO);

        assertEquals(new BigDecimal("30.00"), conta.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarSaqueSemSaldo() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), true);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("15.00"), "Saque"), DONO));

        verify(lancamentoRepository, never()).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarSaqueComContaInativa() {
        UUID contaId = UUID.randomUUID();
        Conta conta = novaConta(contaId, new BigDecimal("10.00"), false);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThrows(NaoProcessavelException.class,
                () -> contaService.sacar(contaId, new SaqueRequestDto(new BigDecimal("1.00"), "Saque"), DONO));
    }

    @Test
    void deveTransferirEntreContasERegistrarLancamento() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        Conta origem = novaConta(origemId, new BigDecimal("100.00"), true);
        Conta destino = novaConta(destinoId, new BigDecimal("20.00"), true);

        when(contaRepository.findById(eq(origemId))).thenReturn(Optional.of(origem));
        when(contaRepository.findById(eq(destinoId))).thenReturn(Optional.of(destino));

        contaService.transferir(new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "Transferencia"), DONO);

        assertEquals(new BigDecimal("70.00"), origem.getSaldo());
        assertEquals(new BigDecimal("50.00"), destino.getSaldo());
        verify(lancamentoRepository).save(any(Lancamento.class));
    }

    @Test
    void deveRecusarTransferenciaParaMesmaConta() {
        UUID contaId = UUID.randomUUID();

        assertThrows(NaoProcessavelException.class,
                () -> contaService.transferir(new TransferirRequestDto(contaId, contaId, new BigDecimal("10.00"), "x"), DONO));
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
                () -> contaService.transferir(new TransferirRequestDto(origemId, destinoId, new BigDecimal("30.00"), "x"), DONO));
    }

    @Test
    void deveRecusarTransferenciaComValorNulo() {
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();

        assertThrows(NaoProcessavelException.class,
                () -> contaService.transferir(new TransferirRequestDto(origemId, destinoId, null, "x"), DONO));
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

        List<LancamentoResponseDto> response = contaService.listarLancamentos(contaId, DONO);

        assertEquals(1, response.size());
        assertEquals(TipoLancamentoEnum.DEPOSITO, response.getFirst().tipoLancamento());
    }

    @Test
    void deveListarContasSemPaginacao() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findAll()).thenReturn(List.of(conta));

        List<ContaResponseDto> response = contaService.listarTodas(ADMIN);

        assertEquals(1, response.size());
    }

    @Test
    void deveListarContasPaginado() {
        Conta conta = novaConta(UUID.randomUUID(), new BigDecimal("1.00"), true);
        when(contaRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(conta)));

        assertEquals(1, contaService.listarPaginado(PageRequest.of(0, 10), ADMIN).getTotalElements());
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
        ReflectionTestUtils.setField(cliente, "id", DONO_ID);
        conta.setCliente(cliente);
        return conta;
    }
}
