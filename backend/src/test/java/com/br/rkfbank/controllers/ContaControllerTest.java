package com.br.rkfbank.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.dto.request.conta.AberturaContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositoRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferenciaRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.services.ContaService;
import com.br.rkfbank.services.IdempotenciaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ContaController.class)
class ContaControllerTest extends ControllerWebMvcBase {

    private static final UUID CONTA_ID = UUID.fromString("a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9");

    @MockitoBean
    private ContaService contaService;

    @MockitoBean
    private IdempotenciaService idempotenciaService;

    @Test
    void deveRetornar201AoAbrirConta() throws Exception {
        when(contaService.abrir(new AberturaContaRequestDto(TipoContaEnum.CORRENTE), CLIENTE)).thenReturn(contaResponse());

        mockMvc.perform(json(post("/contas"), "{\"tipoConta\":\"CORRENTE\"}").with(tokenDoCliente()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroConta").value("12345678"))
                .andExpect(jsonPath("$.agencia").value("0001"))
                .andExpect(jsonPath("$.saldo").value(0.00));
    }

    @Test
    void tipoDeContaDesconhecidoRespondeErroDoContrato() throws Exception {
        mockMvc.perform(json(post("/contas"), "{\"tipoConta\":\"SALARIO\"}").with(tokenDoCliente()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").exists());
        verifyNoInteractions(contaService);
    }

    @Test
    void semTokenResponde401SemChegarAoController() throws Exception {
        mockMvc.perform(get("/contas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(contaService);
    }

    @Test
    void deveDepositarUmaVezPelaIdempotenciaERetornar204() throws Exception {
        DepositoRequestDto request = new DepositoRequestDto(new BigDecimal("10.00"), "Deposito");
        executaAMovimentacao();

        mockMvc.perform(movimentacao(post("/contas/{id}/depositos", CONTA_ID), "chave-deposito-1",
                        "{\"valor\":10.00,\"descricao\":\"Deposito\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(IdempotenciaService.HEADER_REPETIDA));

        verify(contaService).depositar(CONTA_ID, request, CLIENTE);
        verify(idempotenciaService).executarUmaVez(eq(CLIENTE), eq("chave-deposito-1"), eq(TipoLancamentoEnum.DEPOSITO),
                eq(CONTA_ID + "|null|10|Deposito"), any());
    }

    @Test
    void valorComTresCasasRespondeCampoInvalidoComAMensagemDoCampo() throws Exception {
        mockMvc.perform(movimentacao(post("/contas/{id}/depositos", CONTA_ID), "chave-deposito-2", "{\"valor\":10.555}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("valor"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("valor deve ter no máximo 2 casas decimais"));
        verifyNoInteractions(idempotenciaService, contaService);
    }

    @Test
    void deveSacarUmaVezPelaIdempotenciaERetornar204() throws Exception {
        SaqueRequestDto request = new SaqueRequestDto(new BigDecimal("5.00"), "Saque");
        executaAMovimentacao();

        mockMvc.perform(movimentacao(post("/contas/{id}/saques", CONTA_ID), "chave-saque-1",
                        "{\"valor\":5.00,\"descricao\":\"Saque\"}"))
                .andExpect(status().isNoContent());

        verify(contaService).sacar(CONTA_ID, request, CLIENTE);
    }

    @Test
    void deveTransferirComOrigemEDestinoNaAssinatura() throws Exception {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        TransferenciaRequestDto request = new TransferenciaRequestDto(origem, destino, new BigDecimal("50.00"), null);
        executaAMovimentacao();

        mockMvc.perform(movimentacao(post("/transferencias"), "chave-transf-1",
                        "{\"contaOrigemId\":\"" + origem + "\",\"contaDestinoId\":\"" + destino + "\",\"valor\":50.00}"))
                .andExpect(status().isNoContent());

        verify(contaService).transferir(request, CLIENTE);
        // Descricao nula vira vazia; 50.00 vira 50 (mesma operacao que 50.0).
        verify(idempotenciaService).executarUmaVez(eq(CLIENTE), eq("chave-transf-1"), eq(TipoLancamentoEnum.TRANSFERENCIA),
                eq(origem + "|" + destino + "|50|"), any());
    }

    @Test
    void repeticaoDevolve204ComHeaderSemChamarOService() throws Exception {
        when(idempotenciaService.executarUmaVez(any(), any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(movimentacao(post("/contas/{id}/depositos", CONTA_ID), "chave-repetida-1", "{\"valor\":10.00}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(IdempotenciaService.HEADER_REPETIDA, "true"));

        verifyNoInteractions(contaService);
    }

    @Test
    void valoresIguaisComEscalasDiferentesGeramAMesmaAssinatura() throws Exception {
        executaAMovimentacao();

        mockMvc.perform(movimentacao(post("/contas/{id}/depositos", CONTA_ID), "chave-a-12345", "{\"valor\":10.0,\"descricao\":\"x\"}"));
        mockMvc.perform(movimentacao(post("/contas/{id}/depositos", CONTA_ID), "chave-a-12345", "{\"valor\":10.00,\"descricao\":\"x\"}"));

        ArgumentCaptor<String> assinaturas = ArgumentCaptor.forClass(String.class);
        verify(idempotenciaService, times(2)).executarUmaVez(any(), any(), any(), assinaturas.capture(), any());
        assertEquals(assinaturas.getAllValues().get(0), assinaturas.getAllValues().get(1));
    }

    @Test
    void deveBuscarUmaContaPeloId() throws Exception {
        when(contaService.buscarPorId(CONTA_ID, CLIENTE)).thenReturn(contaResponse());

        mockMvc.perform(get("/contas/{id}", CONTA_ID).with(tokenDoCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONTA_ID.toString()))
                .andExpect(jsonPath("$.tipoConta").value("CORRENTE"));
    }

    @Test
    void idQueNaoEUuidResponde400() throws Exception {
        mockMvc.perform(get("/contas/{id}", "nao-e-uuid").with(tokenDoCliente()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").exists());
        verifyNoInteractions(contaService);
    }

    @Test
    void deveListarLancamentosComStatus200() throws Exception {
        when(contaService.listarLancamentos(CONTA_ID, CLIENTE)).thenReturn(List.of(lancamentoResponse()));

        mockMvc.perform(get("/contas/{id}/lancamentos", CONTA_ID).with(tokenDoCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipoLancamento").value("DEPOSITO"));
    }

    @Test
    void deveListarContasSemPaginacao() throws Exception {
        when(contaService.listarTodas(CLIENTE)).thenReturn(List.of(contaResponse()));

        mockMvc.perform(get("/contas").with(tokenDoCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deveListarContasComPaginacao() throws Exception {
        when(contaService.listarPaginado(PageRequest.of(0, 10), CLIENTE))
                .thenReturn(new PageImpl<>(List.of(contaResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/contas").param("page", "0").param("size", "10").with(tokenDoCliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void deveRejeitarPaginacaoComPaginaNegativa() throws Exception {
        mockMvc.perform(get("/contas").param("page", "-1").param("size", "10").with(tokenDoCliente()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("page"));
    }

    @Test
    void deveRejeitarPaginacaoComTamanhoNaoPositivo() throws Exception {
        mockMvc.perform(get("/contas").param("page", "0").param("size", "0").with(tokenDoCliente()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("size"));
    }

    // Faz o mock da idempotencia rodar a movimentacao recebida, como o servico de verdade faz na primeira chamada.
    private void executaAMovimentacao() {
        when(idempotenciaService.executarUmaVez(any(), any(), any(), any(), any())).thenAnswer(chamada -> {
            chamada.<Runnable>getArgument(4).run();
            return false;
        });
    }

    private static MockHttpServletRequestBuilder movimentacao(MockHttpServletRequestBuilder requisicao, String chave, String corpo) {
        return json(requisicao, corpo).header(IdempotenciaService.HEADER, chave).with(tokenDoCliente());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private ContaResponseDto contaResponse() {
        return new ContaResponseDto(CONTA_ID, CLIENTE_ID, "0001", "12345678", TipoContaEnum.CORRENTE,
                new BigDecimal("0.00"), true);
    }

    private LancamentoResponseDto lancamentoResponse() {
        return new LancamentoResponseDto(UUID.randomUUID(), TipoLancamentoEnum.DEPOSITO, new BigDecimal("10.00"),
                null, UUID.randomUUID(), "Deposito", Instant.now());
    }
}
