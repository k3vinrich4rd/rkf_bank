package com.br.rkfbank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Registra uma linha por requisicao (metodo, rota, status, duracao) e propaga um requestId para
// todos os logs gerados durante ela, evitando logs de "entrada/saida" repetidos em cada endpoint.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroLogRequisicao extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(FiltroLogRequisicao.class);

    static final String HEADER_REQUEST_ID = "X-Request-Id";
    static final String CHAVE_REQUEST_ID = "requestId";

    // O id vindo de fora vai parar no log; restringir o formato impede quebra de linha e injecao de texto.
    private static final Pattern REQUEST_ID_VALIDO = Pattern.compile("[A-Za-z0-9-]{1,64}");

    // OncePerRequestFilter garante uma execucao por requisicao (mesmo com forward/erro interno).
    // ThreadContext e o MDC do Log4j2: um mapa preso a thread atual. O padrao de log do log4j2-spring.xml imprime
    // %X{requestId}, entao TODA linha de log desta requisicao (controller, service, SQL) sai com o mesmo id.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolverRequestId(request);
        ThreadContext.put(CHAVE_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), duracaoMs);
            // Threads do Tomcat sao reaproveitadas: sem limpar, o id vazaria para a proxima requisicao.
            ThreadContext.remove(CHAVE_REQUEST_ID);
        }
    }

    // Reaproveita o id enviado pelo chamador (ex.: frontend) para rastrear a mesma operacao ponta a ponta.
    private static String resolverRequestId(HttpServletRequest request) {
        String recebido = request.getHeader(HEADER_REQUEST_ID);
        if (recebido != null && REQUEST_ID_VALIDO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
