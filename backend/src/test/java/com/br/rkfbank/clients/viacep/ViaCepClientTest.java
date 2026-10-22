package com.br.rkfbank.clients.viacep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ViaCepClientTest {

    private ViaCepClient viaCepClient;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        viaCepClient = new ViaCepClient("https://viacep.com.br/ws", 3000);
        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(viaCepClient, "restClient", restClient);
    }

    @Test
    void deveRetornarEnderecoQuandoViaCepResponderComSucesso() {
        ViaCepResponseDto dto = new ViaCepResponseDto("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308", false);
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ViaCepResponseDto.class))
                .thenReturn(dto);

        ViaCepResponseDto resposta = viaCepClient.buscarPorCep("01001000");

        assertEquals("01001-000", resposta.cep());
        assertEquals("Praca da Se", resposta.logradouro());
    }

    @Test
    void deveLancar422QuandoViaCepResponderErroTrue() {
        when(restClient.get().uri("/{cep}/json/", "99999999").retrieve().onStatus(any(), any()).body(ViaCepResponseDto.class))
                .thenReturn(new ViaCepResponseDto(null, null, null, null, null, null, true));

        assertThrows(RegraNegocioException.class, () -> viaCepClient.buscarPorCep("99999999"));
    }

    @Test
    void deveLancar422QuandoViaCepResponderNulo() {
        when(restClient.get().uri("/{cep}/json/", "00000000").retrieve().onStatus(any(), any()).body(ViaCepResponseDto.class))
                .thenReturn(null);

        assertThrows(RegraNegocioException.class, () -> viaCepClient.buscarPorCep("00000000"));
    }

    @Test
    void deveLancar503QuandoOcorrerFalhaInesperada() {
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ViaCepResponseDto.class))
                .thenThrow(new RuntimeException("timeout"));

        ServicoExternoIndisponivelException ex = assertThrows(ServicoExternoIndisponivelException.class, () -> viaCepClient.buscarPorCep("01001000"));
        assertEquals("Falha ao consultar ViaCEP", ex.getMessage());
    }

    @Test
    void devePropagarServicoExternoExceptionSemAlterarMensagem() {
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ViaCepResponseDto.class))
                .thenThrow(new ServicoExternoIndisponivelException("ViaCEP indisponível"));

        ServicoExternoIndisponivelException ex = assertThrows(ServicoExternoIndisponivelException.class, () -> viaCepClient.buscarPorCep("01001000"));
        assertEquals("ViaCEP indisponível", ex.getMessage());
    }

    @Test
    void deveCobrirLambdaDeOnStatusQueLancaServicoExterno() throws Exception {
        Method lambda = ViaCepClient.class.getDeclaredMethod(
                "lambda$buscarPorCep$0",
                org.springframework.http.HttpRequest.class,
                org.springframework.http.client.ClientHttpResponse.class
        );
        lambda.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> lambda.invoke(viaCepClient, null, null));
        assertTrue(ex.getCause() instanceof ServicoExternoIndisponivelException);
        assertEquals("ViaCEP indisponível", ex.getCause().getMessage());
    }
}
