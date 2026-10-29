package com.br.rkfbank.contas.comprovante;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// A cada 2 s (rkfbank.outbox.intervalo) entrega os comprovantes pendentes. Assincrono: a transferencia ja respondeu
// 204 ao cliente; o comprovante sai depois, por aqui. Classe separada do PublicadorOutbox pelo mesmo motivo da
// RotinaAnonimizacao: @Scheduled chamando @Transactional da propria classe nao passa pelo proxy.
// Com varias instancias, so uma publica por vez (ShedLock, ConfiguracaoRotinas).
@Component
public class RotinaPublicacaoOutbox {

    private final PublicadorOutbox publicadorOutbox;

    public RotinaPublicacaoOutbox(PublicadorOutbox publicadorOutbox) {
        this.publicadorOutbox = publicadorOutbox;
    }

    @Scheduled(fixedDelayString = "${rkfbank.outbox.intervalo:PT2S}")
    @SchedulerLock(name = "publicar-outbox", lockAtMostFor = "PT1M")
    public void executar() {
        publicadorOutbox.publicarPendentes();
    }
}
