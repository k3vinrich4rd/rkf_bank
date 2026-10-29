package com.br.rkfbank.autenticacao;

import com.br.rkfbank.compartilhado.erros.AcessoAlteradoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

// Ultima etapa do Resource Server: o token ja passou na assinatura e na validade; aqui ele vira "usuario logado".
// O JWT sozinho e stateless: um admin rebaixado ou um usuario desativado continuaria entrando ate o token vencer.
// Por isso, a cada requisicao, confere em tb_usuario (1 SELECT pela chave primaria) se o usuario ainda existe, esta
// ativo, tem o MESMO perfil e a MESMA versao de senha do token. Mudou algo depois do login -> 401, e a pessoa entra de novo.
//
// Senha temporaria (V14, redefinida pelo ADMIN): o usuario passa, mas so com a autoridade SENHA_TEMPORARIA, sem o
// perfil. A ConfiguracaoSeguranca so libera para ela a troca de senha; o resto e 403 TROCA_DE_SENHA_OBRIGATORIA.
//
// Cache opcional (rkfbank.seguranca.cache-usuario, padrao DESLIGADO): com muito volume, guarda por alguns segundos o
// resultado do SELECT para aquele token. O custo: QUALQUER revogacao (desativar, rebaixar, trocar ou redefinir a
// senha, encerrar o cadastro) passa a valer em ate esse tempo, e nao na hora. Por isso so se liga com volume alto
// e com um prazo curto (ex.: PT5S).
@Component
public class ConversorTokenUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String SENHA_TEMPORARIA = "SENHA_TEMPORARIA";

    private static final Logger log = LogManager.getLogger(ConversorTokenUsuario.class);

    private final UsuarioRepository usuarioRepository;
    private final Duration validadeDoCache;
    private final Clock clock;
    private final JwtAuthenticationConverter conversorPerfil = conversorPerfil();
    private final ConcurrentHashMap<String, Conferido> cache = new ConcurrentHashMap<>();

    // O que o SELECT respondeu, e ate quando a resposta pode ser reaproveitada.
    private record Conferido(boolean valido, boolean senhaTemporaria, Instant validoAte) {
    }

    @Autowired
    public ConversorTokenUsuario(UsuarioRepository usuarioRepository,
                                 @Value("${rkfbank.seguranca.cache-usuario:PT0S}") Duration validadeDoCache) {
        this(usuarioRepository, validadeDoCache, Clock.systemUTC());
    }

    ConversorTokenUsuario(UsuarioRepository usuarioRepository, Duration validadeDoCache, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.validadeDoCache = validadeDoCache;
        this.clock = clock;
    }

    // Chamado pelo Resource Server para cada requisicao com token valido. Devolver a autenticacao = deixar passar;
    // lancar AcessoAlteradoException = 401 com mensagem propria (TratadorErrosSeguranca).
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Conferido conferido = conferir(jwt);
        if (!conferido.valido()) {
            log.warn("Token recusado: usuario {} inexistente, desativado, com perfil ou senha alterados depois do login", jwt.getSubject());
            throw new AcessoAlteradoException();
        }
        if (conferido.senhaTemporaria()) {
            return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(SENHA_TEMPORARIA)));
        }
        return conversorPerfil.convert(jwt);
    }

    private Conferido conferir(Jwt jwt) {
        String perfilNoToken = jwt.getClaimAsString(TokenService.CLAIM_PERFIL);
        int versaoNoToken = versaoNoToken(jwt);
        if (validadeDoCache.isZero()) {
            return consultar(jwt.getSubject(), perfilNoToken, versaoNoToken);
        }
        Instant agora = clock.instant();
        String chave = jwt.getSubject() + "|" + perfilNoToken + "|" + versaoNoToken;
        Conferido guardado = cache.get(chave);
        if (guardado != null && agora.isBefore(guardado.validoAte())) {
            return guardado;
        }
        Conferido novo = consultar(jwt.getSubject(), perfilNoToken, versaoNoToken);
        cache.put(chave, new Conferido(novo.valido(), novo.senhaTemporaria(), agora.plus(validadeDoCache)));
        if (cache.size() > 10_000) {
            cache.values().removeIf(c -> !agora.isBefore(c.validoAte()));
        }
        return novo;
    }

    private Conferido consultar(String subject, String perfilNoToken, int versaoNoToken) {
        Optional<Usuario> usuario = buscar(subject);
        boolean valido = usuario.isPresent() && usuario.get().isAtivo()
                && usuario.get().getPerfil().name().equals(perfilNoToken)
                && usuario.get().getVersaoCredencial() == versaoNoToken;
        return new Conferido(valido, valido && usuario.get().isDeveTrocarSenha(), Instant.MIN);
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
