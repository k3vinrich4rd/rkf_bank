package com.br.rkfbank;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.br.rkfbank.autenticacao.ConfiguracaoSeguranca;
import com.br.rkfbank.autenticacao.PerfilEnum;
import com.br.rkfbank.autenticacao.TokenService;
import com.br.rkfbank.autenticacao.TratadorErrosSeguranca;
import com.br.rkfbank.autenticacao.UsuarioLogado;
import com.br.rkfbank.autenticacao.UsuarioRepository;
import com.br.rkfbank.compartilhado.config.ConfiguracaoCors;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Base dos testes de controller com @WebMvcTest: so a camada web sobe (controller, JSON, Bean Validation,
// TratadorGlobalDeErros e a cadeia de seguranca de verdade), com os services mockados em cada teste.
// O que passa aqui e o que o cliente da API ve: status, corpo JSON, headers e o formato de erro do contrato.
@Import({ConfiguracaoSeguranca.class, ConfiguracaoCors.class, TratadorErrosSeguranca.class})
@ActiveProfiles("test")
public abstract class ControllerWebMvcBase {

    protected static final UUID USUARIO_ID = UUID.fromString("5a1c0f00-0000-4000-8000-0000000000aa");
    protected static final UUID CLIENTE_ID = UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e");
    // O que o controller deve repassar ao service depois de ler o token.
    protected static final UsuarioLogado CLIENTE = new UsuarioLogado(USUARIO_ID, CLIENTE_ID, PerfilEnum.CLIENTE);

    @Autowired
    protected MockMvc mockMvc;

    // O ConversorTokenUsuario entra no slice (e um Converter); o token do jwt() abaixo nao passa por ele.
    @MockitoBean
    protected UsuarioRepository usuarioRepository;

    // Token ja validado, como o Resource Server entrega ao controller: o jwt() pula a assinatura e o
    // ConversorTokenUsuario, mas as regras de rota (hasRole) e o @AuthenticationPrincipal valem de verdade.
    protected static RequestPostProcessor tokenDoCliente() {
        return token(PerfilEnum.CLIENTE, CLIENTE_ID);
    }

    protected static RequestPostProcessor tokenDoAdmin() {
        return token(PerfilEnum.ADMIN, null);
    }

    private static RequestPostProcessor token(PerfilEnum perfil, UUID clienteId) {
        return jwt()
                .jwt(token -> {
                    token.subject(USUARIO_ID.toString()).claim(TokenService.CLAIM_PERFIL, perfil.name());
                    if (clienteId != null) {
                        token.claim(TokenService.CLAIM_CLIENTE_ID, clienteId.toString());
                    }
                })
                .authorities(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }
}
