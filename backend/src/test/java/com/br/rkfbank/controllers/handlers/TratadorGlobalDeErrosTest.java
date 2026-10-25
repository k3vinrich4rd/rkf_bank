package com.br.rkfbank.controllers.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.exceptions.ServicoExternoIndisponivelException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class TratadorGlobalDeErrosTest {

    private TratadorGlobalDeErros handler;

    @BeforeEach
    void setUp() {
        handler = new TratadorGlobalDeErros();
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
        assertEquals("Tipo inválido para o parâmetro id", response.campos().getFirst().mensagem());
    }

    @Test
    void deveUsarMensagemDeTipoCorretaParaParametroNumerico() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalido",
                Integer.class,
                "size",
                null,
                new IllegalArgumentException("numero invalido")
        );

        ErroResponse response = handler.parametroInvalido(ex, request("/api/contas"));

        assertEquals("Tipo inválido para o parâmetro size", response.campos().getFirst().mensagem());
    }

    @Test
    void deveMapearPaginacaoInvalidaPara400Estruturado() {
        ErroResponse response = handler.requisicaoInvalida(
                new RequisicaoInvalidaException("size", "deve estar entre 1 e 100"),
                request("/api/contas")
        );

        assertEquals(400, response.status());
        assertEquals("size", response.campos().getFirst().campo());
    }

    @Test
    void deveMapearConflitoOtimistaPara409() {
        ErroResponse response = handler.conflitoConcorrencia(
                new OptimisticLockingFailureException("stale account"),
                request("/api/contas")
        );

        assertEquals(409, response.status());
    }

    @Test
    void deveMapearConflitoJpaOtimistaPara409() {
        ErroResponse response = handler.conflitoConcorrencia(
                new OptimisticLockException("stale account"),
                request("/api/contas")
        );

        assertEquals(409, response.status());
        assertEquals(CodigoErroEnum.CONCORRENCIA, response.codigo());
    }

    @Test
    void deveMapearClienteNaoEncontradoPara404() {
        ErroResponse response = handler.naoEncontrado(new ClienteNaoEncontradoException(), request("/api/clientes/x"));
        assertEquals(404, response.status());
        assertEquals(CodigoErroEnum.CLIENTE_NAO_ENCONTRADO, response.codigo());
        assertEquals("Cliente não encontrado", response.message());
    }

    @Test
    void deveMapearContaNaoEncontradaPara404() {
        ErroResponse response = handler.naoEncontrado(new ContaNaoEncontradaException(), request("/api/contas/x"));
        assertEquals(404, response.status());
        assertEquals(CodigoErroEnum.CONTA_NAO_ENCONTRADA, response.codigo());
    }

    @Test
    void deveMapearConflitoPara409() {
        ErroResponse response = handler.conflito(new ConflitoException(CodigoErroEnum.CPF_JA_CADASTRADO), request("/api/clientes"));
        assertEquals(409, response.status());
        assertEquals(CodigoErroEnum.CPF_JA_CADASTRADO, response.codigo());
        assertEquals("CPF já cadastrado", response.message());
    }

    @Test
    void deveMapearRegraNegocioPara422ComCodigoEMensagemDoEnum() {
        ErroResponse response = handler.regraNegocio(new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE), request("/api/contas"));
        assertEquals(422, response.status());
        assertEquals(CodigoErroEnum.SALDO_INSUFICIENTE, response.codigo());
        assertEquals("Saldo insuficiente", response.message());
    }

    @Test
    void deveMapearServicoExternoPara503() {
        ErroResponse response = handler.servicoExterno(new ServicoExternoIndisponivelException(), request("/api/clientes"));
        assertEquals(503, response.status());
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
