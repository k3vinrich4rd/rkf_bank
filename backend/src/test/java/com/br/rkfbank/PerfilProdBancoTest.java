package com.br.rkfbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

import com.mysql.cj.conf.ConnectionUrl;
import com.mysql.cj.conf.PropertyDefinitions.SslMode;
import com.mysql.cj.conf.PropertyKey;

// Le o application-prod.properties de verdade e resolve a URL do banco como o Spring faria com as variaveis do
// provedor. O PerfilProdIntegracaoTest troca o MySQL por H2, entao e aqui que a URL publicada e conferida: SSL
// obrigatorio por padrao (o banco gerenciado fica fora da rede do Render) e valor que o MySQL Connector/J aceita.
class PerfilProdBancoTest {

    private static final Map<String, String> VARIAVEIS_OBRIGATORIAS = Map.of(
            "DB_HOST", "banco.example", "DB_NAME", "rkfbank");

    @Test
    void semDbSslModeOSslEObrigatorio() throws IOException {
        String url = urlDoBanco(VARIAVEIS_OBRIGATORIAS);

        assertEquals(SslMode.REQUIRED, sslModeDoDriver(url));
        assertTrue(url.contains("serverTimezone=UTC"), url);
    }

    @Test
    void dbSslModeDoAmbienteVence() throws IOException {
        Map<String, String> variaveis = new HashMap<>(VARIAVEIS_OBRIGATORIAS);
        variaveis.put("DB_SSL_MODE", "VERIFY_CA");

        assertEquals(SslMode.VERIFY_CA, sslModeDoDriver(urlDoBanco(variaveis)));
    }

    @Test
    void hibernateGravaAsDatasEmUtc() throws IOException {
        assertEquals("UTC", perfilProd().getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"));
    }

    private static String urlDoBanco(Map<String, String> variaveis) throws IOException {
        MockEnvironment ambiente = new MockEnvironment();
        variaveis.forEach(ambiente::setProperty);
        return ambiente.resolveRequiredPlaceholders(perfilProd().getProperty("spring.datasource.url"));
    }

    // O proprio parser do driver le o sslMode: valor que ele nao conhece estoura aqui, e nao no deploy.
    private static SslMode sslModeDoDriver(String url) {
        String valor = ConnectionUrl.getConnectionUrlInstance(url, new Properties())
                .getMainHost().getProperty(PropertyKey.sslMode.getKeyName());
        return SslMode.valueOf(valor);
    }

    private static Properties perfilProd() throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));
    }
}
