package com.br.rkfbank.clients.viacep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.ServicoExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ClienteViaCepTest {

    private ClienteViaCep clienteViaCep;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        clienteViaCep = new ClienteViaCep("https://viacep.com.br/ws", 3000);
        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(clienteViaCep, "restClient", restClient);
    }

    @Test
    void deveRetornarEnderecoQuandoViaCepResponderComSucesso() {
        ResponseViaCepDto dto = new ResponseViaCepDto("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308", false);
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ResponseViaCepDto.class))
                .thenReturn(dto);

        ResponseViaCepDto resposta = clienteViaCep.buscarPorCep("01001000");

        assertEquals("01001-000", resposta.cep());
        assertEquals("Praca da Se", resposta.logradouro());
    }

    @Test
    void deveLancar422QuandoViaCepResponderErroTrue() {
        when(restClient.get().uri("/{cep}/json/", "99999999").retrieve().onStatus(any(), any()).body(ResponseViaCepDto.class))
                .thenReturn(new ResponseViaCepDto(null, null, null, null, null, null, true));

        assertThrows(NaoProcessavelException.class, () -> clienteViaCep.buscarPorCep("99999999"));
    }

    @Test
    void deveLancar422QuandoViaCepResponderNulo() {
        when(restClient.get().uri("/{cep}/json/", "00000000").retrieve().onStatus(any(), any()).body(ResponseViaCepDto.class))
                .thenReturn(null);

        assertThrows(NaoProcessavelException.class, () -> clienteViaCep.buscarPorCep("00000000"));
    }

    @Test
    void deveLancar503QuandoOcorrerFalhaInesperada() {
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ResponseViaCepDto.class))
                .thenThrow(new RuntimeException("timeout"));

        ServicoExternoException ex = assertThrows(ServicoExternoException.class, () -> clienteViaCep.buscarPorCep("01001000"));
        assertEquals("Falha ao consultar ViaCEP", ex.getMessage());
    }

    @Test
    void devePropagarServicoExternoExceptionSemAlterarMensagem() {
        when(restClient.get().uri("/{cep}/json/", "01001000").retrieve().onStatus(any(), any()).body(ResponseViaCepDto.class))
                .thenThrow(new ServicoExternoException("ViaCEP indisponível"));

        ServicoExternoException ex = assertThrows(ServicoExternoException.class, () -> clienteViaCep.buscarPorCep("01001000"));
        assertEquals("ViaCEP indisponível", ex.getMessage());
    }

    @Test
    void deveCobrirLambdaDeOnStatusQueLancaServicoExterno() throws Exception {
        Method lambda = ClienteViaCep.class.getDeclaredMethod(
                "lambda$buscarPorCep$0",
                org.springframework.http.HttpRequest.class,
                org.springframework.http.client.ClientHttpResponse.class
        );
        lambda.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> lambda.invoke(clienteViaCep, null, null));
        assertTrue(ex.getCause() instanceof ServicoExternoException);
        assertEquals("ViaCEP indisponível", ex.getCause().getMessage());
    }
}
