package com.br.rkfbank.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

// Servidor de verdade (porta aleatoria), e nao MockMvc: o console do H2 e um servlet proprio, fora do
// DispatcherServlet, e o MockMvc nao o enxerga. O perfil test liga spring.h2.console.enabled, como visitante e equipe.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConsoleH2LigadoTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int porta;

    @Test
    void consoleAbreSemTokenEPermiteFrameDaMesmaOrigem() throws Exception {
        HttpResponse<String> resposta = get("/h2-console/");

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("H2"), "deveria ser a pagina do console do H2");
        assertEquals("SAMEORIGIN", resposta.headers().firstValue("X-Frame-Options").orElse(null));
    }

    @Test
    void apiContinuaFechadaEBloqueandoFrames() throws Exception {
        HttpResponse<String> resposta = get("/contas");

        assertEquals(401, resposta.statusCode());
        assertEquals("DENY", resposta.headers().firstValue("X-Frame-Options").orElse(null));
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build();
        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
