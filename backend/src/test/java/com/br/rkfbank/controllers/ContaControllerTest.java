package com.br.rkfbank.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.services.ContaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ContaControllerTest {

    @Mock
    private ContaService contaService;

    @InjectMocks
    private ContaController contaController;

    @Test
    void deveRetornar201AoAbrirConta() {
        UUID clienteId = UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e");
        AbrirContaRequestDto request = new AbrirContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);
        when(contaService.abrir(clienteId, request)).thenReturn(contaResponse());

        ResponseEntity<ContaResponseDto> response = contaController.abrir(clienteId, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("12345678", response.getBody().numeroConta());
    }

    @Test
    void deveRetornar204AoDepositar() {
        UUID contaId = UUID.randomUUID();
        DepositarRequestDto request = new DepositarRequestDto(new BigDecimal("10.00"), "Deposito");
        doNothing().when(contaService).depositar(contaId, request);

        ResponseEntity<Void> response = contaController.depositar(contaId, request);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deveRetornar204AoSacar() {
        UUID contaId = UUID.randomUUID();
        SaqueRequestDto request = new SaqueRequestDto(new BigDecimal("5.00"), "Saque");
        doNothing().when(contaService).sacar(contaId, request);

        ResponseEntity<Void> response = contaController.sacar(contaId, request);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deveRetornar204AoTransferir() {
        TransferirRequestDto request = new TransferirRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "Transferencia"
        );
        doNothing().when(contaService).transferir(request);

        ResponseEntity<Void> response = contaController.transferir(request);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deveListarLancamentosComStatus200() {
        UUID contaId = UUID.randomUUID();
        when(contaService.listarLancamentos(contaId)).thenReturn(List.of(lancamentoResponse()));

        ResponseEntity<List<LancamentoResponseDto>> response = contaController.listar(contaId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deveListarContasSemPaginacao() {
        when(contaService.listarTodas()).thenReturn(List.of(contaResponse()));

        ResponseEntity<?> response = contaController.listarContas(false, 0, 10);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(List.class, response.getBody());
    }

    @Test
    void deveListarContasComPaginacao() {
        when(contaService.listarPaginado(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(contaResponse()), PageRequest.of(0, 10), 1));

        ResponseEntity<?> response = contaController.listarContas(true, 0, 10);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(Page.class, response.getBody());
    }

    @Test
    void deveRejeitarPaginacaoComPaginaNegativa() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> contaController.listarContas(true, -1, 10)
        );

        assertEquals("page", exception.getCampo());
    }

    @Test
    void deveRejeitarPaginacaoComTamanhoNaoPositivo() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> contaController.listarContas(true, 0, 0)
        );

        assertEquals("size", exception.getCampo());
    }

    private ContaResponseDto contaResponse() {
        return new ContaResponseDto(
                UUID.fromString("a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9"),
                UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e"),
                "0001",
                "12345678",
                TipoContaEnum.CORRENTE,
                new BigDecimal("0.00"),
                true
        );
    }

    private LancamentoResponseDto lancamentoResponse() {
        return new LancamentoResponseDto(
                UUID.randomUUID(),
                TipoLancamentoEnum.DEPOSITO,
                new BigDecimal("10.00"),
                null,
                UUID.randomUUID(),
                "Deposito",
                Instant.now()
        );
    }
}
