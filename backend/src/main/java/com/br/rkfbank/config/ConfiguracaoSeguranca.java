package com.br.rkfbank.config;

import com.br.rkfbank.services.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties({JwtPropriedades.class, AdminPropriedades.class})
public class ConfiguracaoSeguranca {

    // BCrypt direto (custo 10 = 2^10 rodadas): o hash tem sempre 60 caracteres, o tamanho de tb_usuario.senha_hash.
    // O DelegatingPasswordEncoder prefixaria "{bcrypt}" e estouraria a coluna.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Quem pode o que, por rota. Regra de dono (conta de outro cliente) nao cabe aqui: fica no ContaService.
    @Bean
    public SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, RespostaErroSeguranca respostaErro) {
        http
                // CSRF explora cookie enviado sozinho pelo navegador; aqui o token vai no header, entao nao se aplica.
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless: nenhuma sessao no servidor, o JWT viaja em toda requisicao.
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/clientes", "/auth/login").permitAll()
                        // Pagina de erro do Spring: sem isso um 500 numa rota publica viraria 401.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/clientes").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorPerfil()))
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro));
        return http.build();
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

    // A mesma chave assina (login) e confere (cada requisicao): HMAC e simetrico.
    @Bean
    public SecretKey chaveJwt(JwtPropriedades propriedades) {
        String segredo = propriedades.segredo();
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < JwtPropriedades.MIN_BYTES_SEGREDO) {
            // Falha na subida, com recado claro, em vez de aceitar uma chave fraca. O valor nunca entra na mensagem.
            throw new IllegalStateException("JWT_SEGREDO ausente ou curto: use pelo menos "
                    + JwtPropriedades.MIN_BYTES_SEGREDO + " bytes (ex.: openssl rand -base64 48)");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    // Confere assinatura e exp (com 60 s de tolerancia de relogio). Algoritmo fixo: token com outro "alg" e recusado.
    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
