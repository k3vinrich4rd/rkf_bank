package com.br.rkfbank.config;

import com.br.rkfbank.security.ConversorTokenUsuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.SecurityFilterChain;

// Configuracao central do Spring Security. Abordagem: a API e um "Resource Server" do OAuth2. Ela recebe um JWT
// no header Authorization, confere assinatura e validade com a mesma chave que o emitiu (HS256) e decide por
// rota quem pode entrar. Sem sessao no servidor (stateless): cada requisicao se prova sozinha pelo token.
@Configuration
@EnableConfigurationProperties({JwtPropriedades.class, AdminPropriedades.class, EquipePropriedades.class})
public class ConfiguracaoSeguranca {

    // BCrypt direto (custo 10 = 2^10 rodadas): o hash tem sempre 60 caracteres, o tamanho de tb_usuario.senha_hash.
    // O DelegatingPasswordEncoder prefixaria "{bcrypt}" e estouraria a coluna.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Quem pode o que, por rota. Regra de dono (conta de outro cliente) nao cabe aqui: fica no ContaService.
    @Bean
    public SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, TratadorErrosSeguranca respostaErro,
                                                 ConversorTokenUsuario conversorToken) {
        http
                // CSRF desligado DE PROPOSITO (o CodeQL avisa "java/spring-disabled-csrf-protection"; alerta revisado
                // e descartado como falso positivo). CSRF e o ataque em que outro site faz o navegador da vitima mandar
                // uma requisicao, e o navegador anexa SOZINHO o cookie de sessao. Esta API nao tem sessao nem le cookie:
                // toda rota protegida exige "Authorization: Bearer <jwt>", e nenhum navegador poe esse header sozinho.
                // O cookie httpOnly do login fica no servidor do Next (proxy), que e quem monta o header. Se um dia a
                // API passar a autenticar por cookie, o CSRF TEM que voltar (CookieCsrfTokenRepository).
                .csrf(AbstractHttpConfigurer::disable)
                // CORS antes da autenticacao: o preflight (OPTIONS) nao leva token e precisa ser respondido aqui.
                // As regras vem do CorsConfigurationSource da ConfiguracaoCors.
                .cors(Customizer.withDefaults())
                // Stateless: nenhuma sessao no servidor, o JWT viaja em toda requisicao.
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/clientes", "/auth/login").permitAll()
                        // Pagina de erro do Spring: sem isso um 500 numa rota publica viraria 401.
                        .requestMatchers("/error").permitAll()
                        // Health check para Docker/deploy: so UP/DOWN, sem detalhe (show-details=never).
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/clientes").hasRole("ADMIN")
                        // Gestao de usuarios (promover, rebaixar, desativar): so ADMIN.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorToken))
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro));
        return http.build();
    }

    // A mesma chave assina (login) e confere (cada requisicao): HMAC e simetrico.
    @Bean
    public SecretKey chaveJwt(JwtPropriedades propriedades) {
        if (propriedades.segredoAleatorio()) {
            // 48 bytes = 384 bits, acima dos 256 exigidos pelo HS256. Fica so na memoria desta execucao.
            byte[] aleatorio = new byte[48];
            new SecureRandom().nextBytes(aleatorio);
            return new SecretKeySpec(aleatorio, "HmacSHA256");
        }
        String segredo = propriedades.segredo();
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < JwtPropriedades.MIN_BYTES_SEGREDO) {
            // Falha na subida, com recado claro, em vez de aceitar uma chave fraca. O valor nunca entra na mensagem.
            throw new IllegalStateException("JWT_SEGREDO ausente ou curto: use pelo menos "
                    + JwtPropriedades.MIN_BYTES_SEGREDO + " bytes (ex.: openssl rand -base64 48)");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // Quem ASSINA o token no login (TokenService). Nimbus e a biblioteca de JOSE/JWT que o Spring Security usa.
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
