package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Leitura do extrato. Lancamento so e inserido e lido: nenhuma rota altera ou apaga um lancamento.
public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    // Consulta usada no extrato: origem OU destino, ordenado do mais novo para o mais antigo.
    List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);
}