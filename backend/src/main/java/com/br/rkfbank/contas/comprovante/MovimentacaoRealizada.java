package com.br.rkfbank.contas.comprovante;

import com.br.rkfbank.contas.Lancamento;
import com.br.rkfbank.contas.TipoLancamentoEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Evento "o dinheiro se moveu": o que o comprovante precisa, e so isso. LGPD: sem nome, CPF, e-mail nem a descricao
// livre (a pessoa pode escrever qualquer coisa nela). O lancamentoId e o id do evento para o consumidor ser
// idempotente: a entrega e "pelo menos uma vez", e o mesmo lancamento nunca gera dois comprovantes.
public record MovimentacaoRealizada(
        UUID lancamentoId,
        TipoLancamentoEnum tipo,
        UUID contaOrigemId,
        UUID contaDestinoId,
        BigDecimal valor,
        Instant dataHora
) {

    public static final String TIPO_EVENTO = "MovimentacaoRealizada";

    public static MovimentacaoRealizada de(Lancamento lancamento) {
        return new MovimentacaoRealizada(lancamento.getId(), lancamento.getTipoLancamentoEnum(),
                lancamento.getContaOrigemId(), lancamento.getContaDestinoId(), lancamento.getValor(),
                lancamento.getDataHora());
    }
}
