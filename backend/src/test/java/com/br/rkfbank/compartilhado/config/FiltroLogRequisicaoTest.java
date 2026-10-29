package com.br.rkfbank.compartilhado.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltroLogRequisicaoTest {

    private final FiltroLogRequisicao filtro = new FiltroLogRequisicao();

    @Test
    void deveReaproveitarRequestIdValidoEnviadoPeloChamador() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clientes");
        request.addHeader(FiltroLogRequisicao.HEADER_REQUEST_ID, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idDuranteRequisicao = new AtomicReference<>();

        filtro.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                idDuranteRequisicao.set(ThreadContext.get(FiltroLogRequisicao.CHAVE_REQUEST_ID));
            }
        });

        assertEquals("abc-123", idDuranteRequisicao.get());
        assertEquals("abc-123", response.getHeader(FiltroLogRequisicao.HEADER_REQUEST_ID));
        assertNull(ThreadContext.get(FiltroLogRequisicao.CHAVE_REQUEST_ID));
    }

    @Test
    void deveGerarNovoRequestIdQuandoHeaderAusente() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/contas"), response, new MockFilterChain());

        String gerado = response.getHeader(FiltroLogRequisicao.HEADER_REQUEST_ID);
        assertNotNull(gerado);
        assertDoesNotThrow(() -> UUID.fromString(gerado));
    }

    @Test
    void deveDescartarRequestIdComCaracteresQuePoderiamForjarLinhasDeLog() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/contas");
        request.addHeader(FiltroLogRequisicao.HEADER_REQUEST_ID, "abc\nlinha-falsa");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertNotEquals("abc\nlinha-falsa", response.getHeader(FiltroLogRequisicao.HEADER_REQUEST_ID));
    }
}
