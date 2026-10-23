package com.br.rkfbank.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.comum.PaginaResponse;
import com.br.rkfbank.dto.request.conta.AberturaContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositoRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferenciaRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.ContaService;
import com.br.rkfbank.services.IdempotenciaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ContaControllerTest {

    @Mock
    private ContaService contaService;

    @Mock
    private IdempotenciaService idempotenciaService;

    @InjectMocks
    private ContaController contaController;

    // Token ja validado, como o Resource Server entrega ao controller.
    private static final UUID USUARIO_ID = UUID.fromString("5a1c0f00-0000-4000-8000-0000000000aa");
    private static final UUID CLIENTE_ID = UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e");
    private static final Jwt JWT = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject(USUARIO_ID.toString())
            .claim("perfil", "CLIENTE")
            .claim("clienteId", CLIENTE_ID.toString())
            .build();
    // O que o controller deve repassar ao service depois de ler o JWT.
    private static final UsuarioLogado USUARIO = new UsuarioLogado(USUARIO_ID, CLIENTE_ID, PerfilEnum.CLIENTE);

    @Test
    void deveRetornar201AoAbrirConta() {
        UUID clienteId = UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e");
        AberturaContaRequestDto request = new AberturaContaRequestDto("0001", "12345678", TipoContaEnum.CORRENTE);
        when(contaService.abrir(clienteId, request, USUARIO)).thenReturn(contaResponse());

        ResponseEntity<ContaResponseDto> response = contaController.abrir(clienteId, request, JWT);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("12345678", response.getBody().numeroConta());
    }

    @Test
    void deveDepositarUmaVezPelaIdempotenciaERetornar204() {
        UUID contaId = UUID.randomUUID();
        DepositoRequestDto request = new DepositoRequestDto(new BigDecimal("10.00"), "Deposito");
        executaAMovimentacao();

        ResponseEntity<Void> response = contaController.depositar(contaId, request, "chave-deposito-1", JWT);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getHeaders().getFirst(IdempotenciaService.HEADER_REPETIDA));
        verify(contaService).depositar(contaId, request, USUARIO);
        verify(idempotenciaService).executarUmaVez(eq(USUARIO), eq("chave-deposito-1"), eq(TipoLancamentoEnum.DEPOSITO),
                eq(contaId + "|null|10|Deposito"), any());
    }

    @Test
    void deveSacarUmaVezPelaIdempotenciaERetornar204() {
        UUID contaId = UUID.randomUUID();
        SaqueRequestDto request = new SaqueRequestDto(new BigDecimal("5.00"), "Saque");
        executaAMovimentacao();

        ResponseEntity<Void> response = contaController.sacar(contaId, request, "chave-saque-1", JWT);

        assertEquals(204, response.getStatusCode().value());
        verify(contaService).sacar(contaId, request, USUARIO);
    }

    @Test
    void deveTransferirComOrigemEDestinoNaAssinatura() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        TransferenciaRequestDto request = new TransferenciaRequestDto(origem, destino, new BigDecimal("50.00"), null);
        executaAMovimentacao();

        ResponseEntity<Void> response = contaController.transferir(request, "chave-transf-1", JWT);

        assertEquals(204, response.getStatusCode().value());
        verify(contaService).transferir(request, USUARIO);
        // Descricao nula vira vazia; 50.00 vira 50 (mesma operacao que 50.0).
        verify(idempotenciaService).executarUmaVez(eq(USUARIO), eq("chave-transf-1"), eq(TipoLancamentoEnum.TRANSFERENCIA),
                eq(origem + "|" + destino + "|50|"), any());
    }

    @Test
    void repeticaoDevolve204ComHeaderSemChamarOService() {
        UUID contaId = UUID.randomUUID();
        when(idempotenciaService.executarUmaVez(any(), any(), any(), any(), any())).thenReturn(true);

        ResponseEntity<Void> response = contaController.depositar(contaId,
                new DepositoRequestDto(new BigDecimal("10.00"), null), "chave-repetida-1", JWT);

        assertEquals(204, response.getStatusCode().value());
        assertEquals("true", response.getHeaders().getFirst(IdempotenciaService.HEADER_REPETIDA));
        verifyNoInteractions(contaService);
    }

    @Test
    void valoresIguaisComEscalasDiferentesGeramAMesmaAssinatura() {
        UUID contaId = UUID.randomUUID();
        executaAMovimentacao();

        contaController.depositar(contaId, new DepositoRequestDto(new BigDecimal("10.0"), "x"), "chave-a-12345", JWT);
        contaController.depositar(contaId, new DepositoRequestDto(new BigDecimal("10.00"), "x"), "chave-a-12345", JWT);

        ArgumentCaptor<String> assinaturas = ArgumentCaptor.forClass(String.class);
        verify(idempotenciaService, times(2)).executarUmaVez(any(), any(), any(), assinaturas.capture(), any());
        assertEquals(assinaturas.getAllValues().get(0), assinaturas.getAllValues().get(1));
    }

    // Faz o mock da idempotencia rodar a movimentacao recebida, como o servico de verdade faz na primeira chamada.
    private void executaAMovimentacao() {
        when(idempotenciaService.executarUmaVez(any(), any(), any(), any(), any())).thenAnswer(chamada -> {
            chamada.<Runnable>getArgument(4).run();
            return false;
        });
    }

    @Test
    void deveListarLancamentosComStatus200() {
        UUID contaId = UUID.randomUUID();
        when(contaService.listarLancamentos(contaId, USUARIO)).thenReturn(List.of(lancamentoResponse()));

        ResponseEntity<List<LancamentoResponseDto>> response = contaController.extrato(contaId, JWT);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deveListarContasSemPaginacao() {
        when(contaService.listarTodas(USUARIO)).thenReturn(List.of(contaResponse()));

        ResponseEntity<?> response = contaController.listar(false, 0, 10, JWT);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(List.class, response.getBody());
    }

    @Test
    void deveListarContasComPaginacao() {
        when(contaService.listarPaginado(PageRequest.of(0, 10), USUARIO))
                .thenReturn(new PageImpl<>(List.of(contaResponse()), PageRequest.of(0, 10), 1));

        ResponseEntity<?> response = contaController.listar(true, 0, 10, JWT);

        assertEquals(200, response.getStatusCode().value());
        PaginaResponse<?> pagina = assertInstanceOf(PaginaResponse.class, response.getBody());
        assertEquals(1, pagina.content().size());
        assertEquals(1, pagina.totalElements());
        assertEquals(0, pagina.number());
    }

    @Test
    void deveRejeitarPaginacaoComPaginaNegativa() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> contaController.listar(true, -1, 10, JWT)
        );

        assertEquals("page", exception.getCampo());
    }

    @Test
    void deveRejeitarPaginacaoComTamanhoNaoPositivo() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> contaController.listar(true, 0, 0, JWT)
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
