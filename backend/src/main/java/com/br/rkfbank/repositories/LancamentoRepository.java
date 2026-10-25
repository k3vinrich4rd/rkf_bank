package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Leitura do extrato. Lancamento so e inserido e lido por rota: nenhuma rota altera ou apaga um lancamento. A unica
// escrita depois da insercao e a da LGPD (limparDescricoesEscritasPor), que apaga texto livre, nunca valor ou conta.
public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    // Consulta usada no extrato: origem OU destino, ordenado do mais novo para o mais antigo.
    List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);

    // LGPD: a descricao e texto livre ("Aluguel do Joao") e pode identificar alguem. Apaga as que o PROPRIO titular
    // escreveu: saques e transferencias que sairam das contas dele e depositos que entraram nelas. A descricao de uma
    // transferencia que OUTRO cliente mandou para ele e do outro cliente e continua no extrato de quem enviou.
    @Modifying
    @Query("""
            update Lancamento l set l.descricao = null
            where l.descricao is not null
              and (l.contaOrigemId in :contas or (l.contaOrigemId is null and l.contaDestinoId in :contas))""")
    int limparDescricoesEscritasPor(@Param("contas") Collection<UUID> contas);
}