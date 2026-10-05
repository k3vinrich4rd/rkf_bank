package com.br.rkfbank.clients.viacep;

import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.ServicoExternoException;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClienteViaCep {

    private final RestClient restClient;

    public ClienteViaCep(@Value("${viacep.base-url}") String baseUrl, @Value("${viacep.timeout-ms}") int timeoutMs) {
        // Timeouts centralizados para evitar requisicoes penduradas no servico externo.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ResponseViaCepDto buscarPorCep(String cep) {
        try {
            // Busca o CEP remoto e valida status 5xx como indisponibilidade externa.
            ResponseViaCepDto resposta = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ServicoExternoException("ViaCEP indisponível");
                    })
                    .body(ResponseViaCepDto.class);

            // ViaCEP retorna "erro=true" para CEP inexistente mesmo com status HTTP 200.
            if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
                throw new NaoProcessavelException("CEP não encontrado");
            }
            return resposta;
        } catch (NaoProcessavelException | ServicoExternoException ex) {
            // Mantem excecoes de negocio/infra mapeadas sem sobrescrever mensagem.
            throw ex;
        } catch (RuntimeException ex) {
            // Qualquer falha inesperada vira erro de servico externo padronizado.
            throw new ServicoExternoException("Falha ao consultar ViaCEP");
        }
    }
}