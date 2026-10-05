package com.br.rkfbank.controllers.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.exceptions.ServicoExternoException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ControllerExceptionHandlerTest {

    private ControllerExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ControllerExceptionHandler();
    }

    @Test
    void deveMapearValidacaoPara400() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "cpf", "cpf invalido"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, result);

        ErroResponse response = handler.campoInvalido(ex, request("/api/clientes"));

        assertEquals(400, response.status());
        assertEquals("cpf", response.campos().getFirst().campo());
    }

    @Test
    void deveMapearJsonInvalidoPara400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("json invalido", mock(HttpInputMessage.class));

        ErroResponse response = handler.jsonInvalido(ex, request("/api/clientes"));

        assertEquals(400, response.status());
    }

    @Test
    void deveMapearTypeMismatchPara400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "nao-uuid",
                UUID.class,
                "id",
                null,
                new IllegalArgumentException("uuid invalido")
        );

        ErroResponse response = handler.parametroInvalido(ex, request("/api/clientes/abc"));

        assertEquals(400, response.status());
        assertEquals("id", response.campos().getFirst().campo());
    }

    @Test
    void deveMapearClienteNaoEncontradoPara404() {
        ErroResponse response = handler.clienteNaoEncontrado(new ClienteNaoEncontradoException(), request("/api/clientes/x"));
        assertEquals(404, response.status());
    }

    @Test
    void deveMapearContaNaoEncontradaPara404() {
        ErroResponse response = handler.contaNaoEncontrada(new ContaNaoEncontradaException(), request("/api/contas/x"));
        assertEquals(404, response.status());
    }

    @Test
    void deveMapearConflitoPara409() {
        ErroResponse response = handler.conflito(new NegocioException("conflito"), request("/api/clientes"));
        assertEquals(409, response.status());
    }

    @Test
    void deveMapearNaoProcessavelPara422() {
        ErroResponse response = handler.naoProcessavel(new NaoProcessavelException("regra"), request("/api/contas"));
        assertEquals(422, response.status());
    }

    @Test
    void deveMapearServicoExternoPara503() {
        ErroResponse response = handler.servicoExterno(new ServicoExternoException("via cep"), request("/api/clientes"));
        assertEquals(503, response.status());
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
