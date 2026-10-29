package com.br.rkfbank.autenticacao;

import com.br.rkfbank.compartilhado.config.LogSeguro;
import com.br.rkfbank.compartilhado.erros.AcessoAlteradoException;
import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import com.br.rkfbank.compartilhado.web.ErroResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// 401 e 403 barrados pelos filtros do Spring Security, no mesmo formato do TratadorGlobalDeErros.
// O advice nao serve aqui: o filtro recusa a requisicao antes de ela chegar ao DispatcherServlet.
@Component
public class TratadorErrosSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LogManager.getLogger(TratadorErrosSeguranca.class);

    // Poe o header "WWW-Authenticate: Bearer ..." exigido pela RFC 6750; o corpo JSON e nosso.
    private final BearerTokenAuthenticationEntryPoint cabecalhoBearer = new BearerTokenAuthenticationEntryPoint();
    private final JsonMapper jsonMapper;

    public TratadorErrosSeguranca(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 401: sem token, token mal formado, assinatura errada ou expirado.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        CodigoErroEnum codigo = ex instanceof AcessoAlteradoException ? CodigoErroEnum.ACESSO_ALTERADO
                : ex instanceof InvalidBearerTokenException ? CodigoErroEnum.TOKEN_INVALIDO
                : CodigoErroEnum.AUTENTICACAO_NECESSARIA;
        // Nunca o token no log: com ele, qualquer um entra como o usuario ate expirar.
        log.warn("401 em {}: {}", LogSeguro.texto(request.getRequestURI()), ex.getClass().getSimpleName());
        cabecalhoBearer.commence(request, response, ex);
        escrever(response, HttpStatus.UNAUTHORIZED, codigo, request.getRequestURI());
    }

    // 403: logado, mas o perfil nao tem acesso a rota (ex.: CLIENTE em GET /clientes).
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        // Senha temporaria (redefinida pelo ADMIN): o token so tem a autoridade SENHA_TEMPORARIA e so troca a senha.
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        boolean senhaTemporaria = autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(a -> ConversorTokenUsuario.SENHA_TEMPORARIA.equals(a.getAuthority()));
        CodigoErroEnum codigo = senhaTemporaria ? CodigoErroEnum.TROCA_DE_SENHA_OBRIGATORIA : CodigoErroEnum.PERFIL_SEM_PERMISSAO;
        log.warn("403 em {}: {}", LogSeguro.texto(request.getRequestURI()), codigo);
        escrever(response, HttpStatus.FORBIDDEN, codigo, request.getRequestURI());
    }

    private void escrever(HttpServletResponse response, HttpStatus status, CodigoErroEnum codigo, String path)
            throws IOException {
        ErroResponse corpo = new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo, codigo.getMensagem(), path, List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), corpo);
    }
}
