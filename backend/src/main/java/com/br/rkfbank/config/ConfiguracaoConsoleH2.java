package com.br.rkfbank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

// Console web do H2 (http://localhost:8080/h2-console): uma tela para olhar as tabelas do banco em memoria enquanto
// se testa a API. So existe nos perfis locais com H2 (visitante, equipe e test), que ligam spring.h2.console.enabled.
// No perfil padrao (MySQL) a propriedade fica desligada, este @Configuration nem e criado e /h2-console cai na
// cadeia principal (401 sem token, 404 com token): o console nunca vai para um ambiente de verdade.
//
// Por que uma SEGUNDA cadeia de filtros, e nao mais um permitAll na ConfiguracaoSeguranca: o console e uma aplicacao
// web a parte, com regras opostas as da API. Ele usa sessao, formulario (POST com cookie) e paginas dentro de
// <frame>. A cadeia principal desliga sessao e manda "X-Frame-Options: DENY" (bloqueia frames). Separar deixa cada
// regra no seu lugar: a API continua stateless e travada, e o relaxamento vale SO para o caminho do console.
//
// Quem protege o console, entao? (1) ele so existe em perfil local; (2) o H2 so aceita conexao da propria maquina
// (spring.h2.console.settings.web-allow-others=false, o padrao); (3) o banco e em memoria e some ao parar a API.
@Configuration
@ConditionalOnBooleanProperty("spring.h2.console.enabled")
public class ConfiguracaoConsoleH2 {

    // @Order(1): o Spring Security testa as cadeias em ordem e usa a primeira cujo securityMatcher casa.
    // A cadeia principal nao tem matcher (casa com tudo), entao precisa vir depois desta.
    @Bean
    @Order(1)
    public SecurityFilterChain cadeiaConsoleH2(HttpSecurity http,
                                               @Value("${spring.h2.console.path:/h2-console}") String caminho)
            throws Exception {
        http
                .securityMatcher(caminho + "/**")
                .authorizeHttpRequests(rotas -> rotas.anyRequest().permitAll())
                // O formulario de login do console nao manda token CSRF; aqui so ha banco local de teste. A excecao vale
                // so para o caminho do console (e nao um disable() geral), que e o unico que esta cadeia atende.
                .csrf(csrf -> csrf.ignoringRequestMatchers(caminho + "/**"))
                // O console monta a tela com <frame> da mesma origem: DENY deixaria a pagina em branco.
                .headers(cabecalhos -> cabecalhos.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
