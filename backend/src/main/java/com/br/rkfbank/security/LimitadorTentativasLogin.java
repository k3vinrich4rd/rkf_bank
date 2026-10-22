package com.br.rkfbank.security;

import com.br.rkfbank.exceptions.MuitasTentativasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Freia o chute de senha: 5 logins errados para o mesmo CPF dentro de 15 min bloqueiam aquele CPF ate a janela
// acabar (429). Acertou a senha, o contador zera. Fica em memoria: reiniciar a API zera tudo, e com mais de uma
// instancia cada uma conta a sua parte (quando houver, trocar por Redis ou tabela). O CPF nunca vai para o log.
@Component
public class LimitadorTentativasLogin {

    private static final Logger log = LogManager.getLogger(LimitadorTentativasLogin.class);

    static final int MAX_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    // Teto de CPFs acompanhados: um ataque com milhoes de CPFs diferentes nao esgota a memoria.
    private static final int MAX_CPFS_ACOMPANHADOS = 10_000;

    // Quantas vezes errou e quando foi a primeira: a janela de 15 min conta a partir do primeiro erro.
    private record Falhas(int quantidade, Instant primeira) {
    }

    private final Clock clock;
    // ConcurrentHashMap: varias requisicoes de login rodam ao mesmo tempo (uma thread cada) mexendo no mesmo mapa.
    // Um HashMap comum poderia perder contagens ou corromper a estrutura nesse caso.
    private final ConcurrentHashMap<String, Falhas> falhas = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorTentativasLogin() {
        this(Clock.systemUTC());
    }

    // Construtor dos testes: com um Clock fixo, o teste "avanca o relogio" 15 min sem esperar de verdade.
    LimitadorTentativasLogin(Clock clock) {
        this.clock = clock;
    }

    // Chamado ANTES de conferir a senha: bloqueado nao gasta BCrypt nem descobre se a senha estava certa.
    public void conferir(String login) {
        Falhas atual = falhas.get(login);
        if (atual == null) {
            return;
        }
        Instant fimDaJanela = atual.primeira().plus(JANELA);
        Instant agora = clock.instant();
        if (!agora.isBefore(fimDaJanela)) {
            falhas.remove(login, atual);
            return;
        }
        if (atual.quantidade() >= MAX_FALHAS) {
            long segundos = Math.max(1, Duration.between(agora, fimDaJanela).toSeconds());
            log.warn("Login bloqueado por excesso de tentativas ({} falhas); libera em {} s", atual.quantidade(), segundos);
            throw new MuitasTentativasException(segundos);
        }
    }

    // Senha errada: soma 1 dentro da janela, ou comeca uma janela nova se a anterior ja venceu.
    // merge() faz ler-somar-gravar de uma vez so (atomico), sem duas threads contarem o mesmo erro.
    public void registrarFalha(String login) {
        if (falhas.size() >= MAX_CPFS_ACOMPANHADOS) {
            limparVencidos();
        }
        Instant agora = clock.instant();
        falhas.merge(login, new Falhas(1, agora), (antiga, nova) ->
                agora.isBefore(antiga.primeira().plus(JANELA))
                        ? new Falhas(antiga.quantidade() + 1, antiga.primeira())
                        : nova);
    }

    // Login certo: esquece os erros daquele CPF.
    public void limpar(String login) {
        falhas.remove(login);
    }

    private void limparVencidos() {
        Instant limite = clock.instant().minus(JANELA);
        falhas.entrySet().removeIf(entrada -> entrada.getValue().primeira().isBefore(limite));
    }
}
