package com.br.rkfbank.contas.comprovante;

// Porta de saida: "avise o cliente que o dinheiro se moveu". Hoje o adaptador e o NotificadorNoLog; e-mail, push ou
// um broker (RabbitMQ, SNS) entram como outro adaptador, sem mexer no ContaService nem no PublicadorOutbox.
// Lancou excecao = nao entregou: o publicador conta a tentativa e tenta de novo na proxima rodada.
public interface NotificadorDeMovimentacao {

    void notificar(MovimentacaoRealizada movimentacao);
}
