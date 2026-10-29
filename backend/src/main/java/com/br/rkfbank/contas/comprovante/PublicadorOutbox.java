package com.br.rkfbank.contas.comprovante;

import com.br.rkfbank.compartilhado.config.LimpezaPeriodica;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

// Lado da leitura do Outbox: entrega os eventos pendentes ao NotificadorDeMovimentacao, FORA da requisicao.
// Quem chama e a RotinaPublicacaoOutbox (a cada 2 s). Cada evento e tratado sozinho: um que falha conta uma tentativa
// e nao segura os outros. Garantia "pelo menos uma vez": se a API cair depois de notificar e antes do commit, o evento
// sai de novo na proxima rodada (por isso o consumidor usa o lancamentoId para nao repetir o comprovante).
@Service
public class PublicadorOutbox implements LimpezaPeriodica {

    private static final Logger log = LogManager.getLogger(PublicadorOutbox.class);

    // Publicados ha mais de 30 dias saem na limpeza diaria; os que desistiram ficam para o ADMIN investigar.
    static final Duration RETENCAO_DOS_PUBLICADOS = Duration.ofDays(30);

    private final OutboxRepository outboxRepository;
    private final NotificadorDeMovimentacao notificador;
    private final JsonMapper jsonMapper;

    public PublicadorOutbox(OutboxRepository outboxRepository, NotificadorDeMovimentacao notificador,
                            JsonMapper jsonMapper) {
        this.outboxRepository = outboxRepository;
        this.notificador = notificador;
        this.jsonMapper = jsonMapper;
    }

    // Devolve quantos foram entregues nesta rodada (os testes e o log usam).
    @Transactional
    public int publicarPendentes() {
        List<EventoOutbox> pendentes = outboxRepository
                .findTop100ByPublicadoEmIsNullAndTentativasLessThanOrderByCriadoEmAsc(EventoOutbox.MAX_TENTATIVAS);
        int entregues = 0;
        for (EventoOutbox evento : pendentes) {
            try {
                notificador.notificar(jsonMapper.readValue(evento.getPayload(), MovimentacaoRealizada.class));
                evento.marcarPublicado(Instant.now());
                entregues++;
            } catch (RuntimeException ex) {
                evento.registrarFalha(ex);
                if (evento.desistiu()) {
                    log.error("Outbox: evento {} desistiu depois de {} tentativas ({})", evento.getId(),
                            evento.getTentativas(), evento.getUltimoErro());
                } else {
                    log.warn("Outbox: evento {} falhou (tentativa {} de {}): {}", evento.getId(),
                            evento.getTentativas(), EventoOutbox.MAX_TENTATIVAS, evento.getUltimoErro());
                }
            }
        }
        if (!pendentes.isEmpty()) {
            log.debug("Outbox: {} de {} evento(s) entregue(s)", entregues, pendentes.size());
        }
        return entregues;
    }

    // LimpezaPeriodica (RotinaLimpezaDiaria, 04h): publicados ha mais de 30 dias.
    @Override
    @Transactional
    public void limparVencidos(Instant agora) {
        int apagados = outboxRepository.apagarPublicadosAntesDe(agora.minus(RETENCAO_DOS_PUBLICADOS));
        log.info("Limpeza do outbox: {} evento(s) publicado(s) ha mais de {} dias apagado(s)", apagados,
                RETENCAO_DOS_PUBLICADOS.toDays());
    }
}
