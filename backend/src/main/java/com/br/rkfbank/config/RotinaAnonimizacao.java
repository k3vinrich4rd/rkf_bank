package com.br.rkfbank.config;

import com.br.rkfbank.services.TitularService;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Todo dia (padrao 03:00, horario de Sao Paulo) anonimiza quem encerrou o cadastro ha mais tempo que a retencao.
// Fica numa classe separada do TitularService de proposito: @Scheduled chamando um metodo @Transactional da
// PROPRIA classe nao passa pelo proxy do Spring, e a transacao nao abriria.
// Com varias instancias, so uma roda (ShedLock, ConfiguracaoRotinas).
@Component
@EnableConfigurationProperties(LgpdPropriedades.class)
public class RotinaAnonimizacao {

    private static final Logger log = LogManager.getLogger(RotinaAnonimizacao.class);

    private final TitularService titularService;

    public RotinaAnonimizacao(TitularService titularService) {
        this.titularService = titularService;
    }

    @Scheduled(cron = "${rkfbank.lgpd.cron-anonimizacao:0 0 3 * * *}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "anonimizacao-lgpd", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void executar() {
        int anonimizados = titularService.anonimizarVencidos(Instant.now());
        log.info("Rotina LGPD: {} cadastro(s) encerrado(s) anonimizado(s)", anonimizados);
    }
}
