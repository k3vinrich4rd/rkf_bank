package com.br.rkfbank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

// CORS: quais sites (origens) o navegador deixa chamar a API direto. O frontend de hoje passa pelo proxy /api do
// Next (mesma origem, sem CORS); isto vale para quem chamar a API sem o proxy (outro front, app, deploy separado).
// As rotas da API nao tem prefixo /api, por isso o mapeamento e "/**". O Spring Security le este bean pelo
// http.cors(...) da ConfiguracaoSeguranca: sem isso o preflight (OPTIONS) levaria 401 antes de chegar aqui.
@Configuration
public class ConfiguracaoCors {

    // O nome importa: o http.cors(...) do Spring Security procura o bean "corsConfigurationSource".
    // Com outro nome ele cai no CORS do Spring MVC, que esta vazio, e nenhuma regra vale.
    @Bean(name = "corsConfigurationSource")
    public CorsConfigurationSource fonteCors(@Value("${rkfbank.cors.origens-permitidas}") List<String> origensPermitidas) {
        CorsConfiguration regras = new CorsConfiguration();
        // Origem exata (sem "*"): com Authorization no header, liberar qualquer site seria abrir a API para todos.
        regras.setAllowedOrigins(origensPermitidas);
        regras.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        regras.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", FiltroLogRequisicao.HEADER_REQUEST_ID));
        // Headers que o JavaScript do navegador so consegue ler se forem expostos.
        regras.setExposedHeaders(List.of(FiltroLogRequisicao.HEADER_REQUEST_ID, "Idempotency-Replayed", "Retry-After"));
        // O navegador guarda a resposta do preflight por 1 h em vez de repetir o OPTIONS a cada chamada.
        regras.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", regras);
        return fonte;
    }
}
