package com.br.rkfbank.clients.viacep;

import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ServicoExternoIndisponivelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// Cliente HTTP do ViaCEP (servico publico que devolve o endereco a partir do CEP).
// Abordagem: a integracao externa fica isolada numa classe so. O ClienteService nao sabe que existe HTTP,
// timeout ou JSON do ViaCEP; ele so pede "o endereco deste CEP". Trocar de fornecedor ou simular nos testes
// (@MockitoBean) mexe so aqui. RestClient e o cliente HTTP sincrono e fluente do Spring 6+, sucessor do RestTemplate.
@Component
public class ViaCepClient {

    private static final Logger log = LogManager.getLogger(ViaCepClient.class);

    private final RestClient restClient;

    public ViaCepClient(@Value("${viacep.base-url}") String baseUrl, @Value("${viacep.timeout-ms}") int timeoutMs) {
        // Timeouts centralizados para evitar requisicoes penduradas no servico externo.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    // Traduz cada resultado do ViaCEP para a linguagem da API: achou -> endereco; CEP inexistente -> 422;
    // fora do ar, 5xx ou demorou mais que o timeout -> 503. O tempo de cada consulta vai para o log (DEBUG/WARN),
    // o que ajuda a descobrir se a lentidao e nossa ou do servico externo.
    public ViaCepResponseDto buscarPorCep(String cep) {
        long inicio = System.nanoTime();
        try {
            // Busca o CEP remoto e valida status 5xx como indisponibilidade externa.
            ViaCepResponseDto resposta = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ServicoExternoIndisponivelException("ViaCEP indisponível");
                    })
                    .body(ViaCepResponseDto.class);

            // ViaCEP retorna "erro=true" para CEP inexistente mesmo com status HTTP 200.
            if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
                throw new RegraNegocioException("CEP não encontrado");
            }
            log.debug("ViaCEP respondeu CEP {} em {} ms", cep, duracaoMs(inicio));
            return resposta;
        } catch (RegraNegocioException ex) {
            log.warn("ViaCEP nao encontrou o CEP {} ({} ms)", cep, duracaoMs(inicio));
            throw ex;
        } catch (ServicoExternoIndisponivelException ex) {
            // Mantem excecoes de infra mapeadas sem sobrescrever mensagem.
            log.error("ViaCEP indisponivel ao consultar CEP {} ({} ms)", cep, duracaoMs(inicio));
            throw ex;
        } catch (RuntimeException ex) {
            // Qualquer falha inesperada vira erro de servico externo padronizado; a causa fica no log.
            log.error("Falha ao consultar ViaCEP para CEP {} ({} ms)", cep, duracaoMs(inicio), ex);
            throw new ServicoExternoIndisponivelException("Falha ao consultar ViaCEP", ex);
        }
    }

    private static long duracaoMs(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
