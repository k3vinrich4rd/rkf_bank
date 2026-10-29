package com.br.rkfbank.contas.comprovante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.contas.TipoLancamentoEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// Lado da leitura do Outbox: entrega, conta as falhas e desiste depois de 5, sem um evento travar os outros.
class PublicadorOutboxTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private OutboxRepository outboxRepository;
    private NotificadorDeMovimentacao notificador;
    private PublicadorOutbox publicador;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        notificador = mock(NotificadorDeMovimentacao.class);
        publicador = new PublicadorOutbox(outboxRepository, notificador, jsonMapper);
    }

    @Test
    void deveEntregarEMarcarComoPublicado() {
        MovimentacaoRealizada transferencia = movimentacao(new BigDecimal("25.00"));
        EventoOutbox evento = evento(transferencia);
        pendentes(evento);

        assertEquals(1, publicador.publicarPendentes());

        verify(notificador).notificar(transferencia);
        assertTrue(evento.isPublicado());
        assertNull(evento.getUltimoErro());
    }

    @Test
    void falhaContaUmaTentativaENaoSeguraOProximo() {
        MovimentacaoRealizada quebra = movimentacao(new BigDecimal("1.00"));
        MovimentacaoRealizada passa = movimentacao(new BigDecimal("2.00"));
        EventoOutbox primeiro = evento(quebra);
        EventoOutbox segundo = evento(passa);
        pendentes(primeiro, segundo);
        doThrow(new IllegalStateException("servidor de e-mail fora")).when(notificador)
                .notificar(argThat(m -> m != null && m.valor().compareTo(BigDecimal.ONE) == 0));

        assertEquals(1, publicador.publicarPendentes());

        assertFalse(primeiro.isPublicado());
        assertEquals(1, primeiro.getTentativas());
        // So a classe do erro: a mensagem pode trazer dado de quem chamou.
        assertEquals("IllegalStateException", primeiro.getUltimoErro());
        assertTrue(segundo.isPublicado());
    }

    @Test
    void desisteDepoisDeCincoFalhas() {
        EventoOutbox evento = evento(movimentacao(BigDecimal.TEN));
        doThrow(new IllegalStateException()).when(notificador).notificar(any());

        for (int rodada = 1; rodada <= EventoOutbox.MAX_TENTATIVAS; rodada++) {
            pendentes(evento);
            publicador.publicarPendentes();
        }

        assertEquals(EventoOutbox.MAX_TENTATIVAS, evento.getTentativas());
        assertTrue(evento.desistiu());
    }

    @Test
    void payloadQuebradoTambemContaComoFalha() {
        EventoOutbox evento = new EventoOutbox(MovimentacaoRealizada.TIPO_EVENTO, "{nao e json");
        pendentes(evento);

        assertEquals(0, publicador.publicarPendentes());

        assertEquals(1, evento.getTentativas());
        assertNotNull(evento.getUltimoErro());
    }

    @Test
    void limpezaApagaSoPublicadosComMaisDe30Dias() {
        Instant agora = Instant.parse("2026-10-29T07:00:00Z");

        publicador.limparVencidos(agora);

        verify(outboxRepository).apagarPublicadosAntesDe(Instant.parse("2026-09-29T07:00:00Z"));
    }

    private void pendentes(EventoOutbox... eventos) {
        when(outboxRepository.findTop100ByPublicadoEmIsNullAndTentativasLessThanOrderByCriadoEmAsc(
                EventoOutbox.MAX_TENTATIVAS)).thenReturn(List.of(eventos));
    }

    private EventoOutbox evento(MovimentacaoRealizada movimentacao) {
        return new EventoOutbox(MovimentacaoRealizada.TIPO_EVENTO, jsonMapper.writeValueAsString(movimentacao));
    }

    private static MovimentacaoRealizada movimentacao(BigDecimal valor) {
        return new MovimentacaoRealizada(UUID.randomUUID(), TipoLancamentoEnum.TRANSFERENCIA, UUID.randomUUID(),
                UUID.randomUUID(), valor, Instant.parse("2026-10-29T12:00:00Z"));
    }
}
