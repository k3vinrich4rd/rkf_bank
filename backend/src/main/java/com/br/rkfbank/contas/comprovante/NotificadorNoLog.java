package com.br.rkfbank.contas.comprovante;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

// Adaptador do MVP: o "comprovante" sai no log da API, uma linha por movimentacao. Prova o fluxo assincrono de ponta a
// ponta sem depender de servidor de e-mail. So ids, tipo e valor (os mesmos que o log do ContaService ja mostra).
@Component
public class NotificadorNoLog implements NotificadorDeMovimentacao {

    private static final Logger log = LogManager.getLogger(NotificadorNoLog.class);

    @Override
    public void notificar(MovimentacaoRealizada movimentacao) {
        log.info("Comprovante: {} de {} (origem={}, destino={}, lancamento={}, em {})", movimentacao.tipo(),
                movimentacao.valor(), movimentacao.contaOrigemId(), movimentacao.contaDestinoId(),
                movimentacao.lancamentoId(), movimentacao.dataHora());
    }
}
