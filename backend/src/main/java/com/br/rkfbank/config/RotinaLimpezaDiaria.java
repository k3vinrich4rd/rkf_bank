package com.br.rkfbank.config;

import com.br.rkfbank.security.LimitadorTentativasLogin;
import com.br.rkfbank.services.IdempotenciaService;
import com.br.rkfbank.services.RefreshTokenService;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Todo dia (padrao 04:00, horario de Sao Paulo, uma hora depois da anonimizacao) tira do banco o que nao serve mais:
//  - chaves de idempotencia com mais de 30 dias (repeticao de verdade acontece em segundos);
//  - janelas vencidas do limite de tentativas de login (nao bloqueiam mais ninguem);
//  - tokens de renovacao vencidos ha mais de 30 dias.
// Classe separada dos services pelo mesmo motivo da RotinaAnonimizacao: @Scheduled chamando metodo @Transactional
// da propria classe nao passa pelo proxy. Com varias instancias, so uma roda (ShedLock).
@Component
public class RotinaLimpezaDiaria {

    private final IdempotenciaService idempotenciaService;
    private final LimitadorTentativasLogin limitadorTentativasLogin;
    private final RefreshTokenService refreshTokenService;

    public RotinaLimpezaDiaria(IdempotenciaService idempotenciaService, LimitadorTentativasLogin limitadorTentativasLogin,
                               RefreshTokenService refreshTokenService) {
        this.idempotenciaService = idempotenciaService;
        this.limitadorTentativasLogin = limitadorTentativasLogin;
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "${rkfbank.limpeza.cron:0 0 4 * * *}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "limpeza-diaria", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void executar() {
        idempotenciaService.apagarChavesAntigas(Instant.now());
        limitadorTentativasLogin.apagarJanelasVencidas();
        refreshTokenService.apagarVencidos(Instant.now());
    }
}
