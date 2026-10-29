package com.br.rkfbank.compartilhado.config;

import java.time.Instant;

// Cada modulo diz o que tirar do banco na limpeza diaria (RotinaLimpezaDiaria): chaves de idempotencia (contas),
// janelas de tentativa de login e tokens de renovacao vencidos (autenticacao). O compartilhado chama todos sem
// conhecer nenhum: quem depende e o modulo, que implementa esta interface (inversao de dependencia).
public interface LimpezaPeriodica {

    // agora: o mesmo instante para todas as limpezas da rodada. Implementacoes rodam numa transacao propria.
    void limparVencidos(Instant agora);
}
