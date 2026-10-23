package com.br.rkfbank.security;

import com.br.rkfbank.exceptions.AcessoAlteradoException;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.services.TokenService;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

// Ultima etapa do Resource Server: o token ja passou na assinatura e na validade; aqui ele vira "usuario logado".
// O JWT sozinho e stateless: um admin rebaixado ou um usuario desativado continuaria entrando ate o token vencer.
// Por isso, a cada requisicao, confere em tb_usuario (1 SELECT pela chave primaria) se o usuario ainda existe, esta
// ativo, tem o MESMO perfil e a MESMA versao de senha do token. Mudou algo depois do login -> 401, e a pessoa entra de novo.
@Component
public class ConversorTokenUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LogManager.getLogger(ConversorTokenUsuario.class);

    private final UsuarioRepository usuarioRepository;
    private final JwtAuthenticationConverter conversorPerfil = conversorPerfil();

    public ConversorTokenUsuario(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Chamado pelo Resource Server para cada requisicao com token valido. Devolver a autenticacao = deixar passar;
    // lancar AcessoAlteradoException = 401 com mensagem propria (TratadorErrosSeguranca).
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Optional<Usuario> usuario = buscar(jwt.getSubject());
        String perfilNoToken = jwt.getClaimAsString(TokenService.CLAIM_PERFIL);
        if (usuario.isEmpty() || !usuario.get().isAtivo() || !usuario.get().getPerfil().name().equals(perfilNoToken)
                || usuario.get().getVersaoCredencial() != versaoNoToken(jwt)) {
            log.warn("Token recusado: usuario {} inexistente, desativado, com perfil ou senha alterados depois do login", jwt.getSubject());
            throw new AcessoAlteradoException();
        }
        return conversorPerfil.convert(jwt);
    }

    // Token sem a claim (emitido antes da V8) vale como versao 0, a mesma de quem nunca trocou a senha.
    private static int versaoNoToken(Jwt jwt) {
        Object versao = jwt.getClaim(TokenService.CLAIM_VERSAO_CREDENCIAL);
        return versao instanceof Number numero ? numero.intValue() : 0;
    }

    private Optional<Usuario> buscar(String subject) {
        try {
            return usuarioRepository.findById(UUID.fromString(subject));
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Assinado com a nossa chave, mas sem um id valido: nao foi o TokenService que emitiu.
            return Optional.empty();
        }
    }

    // Claim "perfil": "CLIENTE" -> autoridade ROLE_CLIENTE, que e o que hasRole("...") procura.
    private static JwtAuthenticationConverter conversorPerfil() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
