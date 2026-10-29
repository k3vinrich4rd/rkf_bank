package com.br.rkfbank.compartilhado.config;

import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Todo dia (padrao 04:00, horario de Sao Paulo, uma hora depois da anonimizacao) tira do banco o que nao serve mais.
// Quem diz o que apagar e cada modulo, pela interface LimpezaPeriodica:
//  - contas: chaves de idempotencia com mais de 30 dias (repeticao de verdade acontece em segundos);
//  - autenticacao: janelas vencidas do limite de tentativas de login e tokens de renovacao vencidos ha mais de 30 dias.
// Classe separada dos services pelo mesmo motivo da RotinaAnonimizacao: @Scheduled chamando metodo @Transactional
// da propria classe nao passa pelo proxy. Com varias instancias, so uma roda (ShedLock).
@Component
public class RotinaLimpezaDiaria {

    private final List<LimpezaPeriodica> limpezas;

    public RotinaLimpezaDiaria(List<LimpezaPeriodica> limpezas) {
        this.limpezas = limpezas;
    }

    @Scheduled(cron = "${rkfbank.limpeza.cron:0 0 4 * * *}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "limpeza-diaria", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void executar() {
        Instant agora = Instant.now();
        limpezas.forEach(limpeza -> limpeza.limparVencidos(agora));
    }
}
