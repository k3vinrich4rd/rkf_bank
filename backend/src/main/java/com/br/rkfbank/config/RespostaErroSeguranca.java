package com.br.rkfbank.config;

import com.br.rkfbank.dto.comum.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// 401 e 403 barrados pelos filtros do Spring Security, no mesmo formato do ControllerExceptionHandler.
// O advice nao serve aqui: o filtro recusa a requisicao antes de ela chegar ao DispatcherServlet.
@Component
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LogManager.getLogger(RespostaErroSeguranca.class);

    // Poe o header "WWW-Authenticate: Bearer ..." exigido pela RFC 6750; o corpo JSON e nosso.
    private final BearerTokenAuthenticationEntryPoint cabecalhoBearer = new BearerTokenAuthenticationEntryPoint();
    private final JsonMapper jsonMapper;

    public RespostaErroSeguranca(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 401: sem token, token mal formado, assinatura errada ou expirado.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String mensagem = ex instanceof InvalidBearerTokenException
                ? "Token invalido ou expirado; faca login novamente"
                : "Autenticacao necessaria: envie o token no header Authorization (Bearer)";
        // Nunca o token no log: com ele, qualquer um entra como o usuario ate expirar.
        log.warn("401 em {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        cabecalhoBearer.commence(request, response, ex);
        escrever(response, HttpStatus.UNAUTHORIZED, mensagem, request.getRequestURI());
    }

    // 403: logado, mas o perfil nao tem acesso a rota (ex.: CLIENTE em GET /clientes).
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        log.warn("403 em {}: perfil sem permissao", request.getRequestURI());
        escrever(response, HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil", request.getRequestURI());
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String mensagem, String path)
            throws IOException {
        ErroResponse corpo = new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), corpo);
    }
}
