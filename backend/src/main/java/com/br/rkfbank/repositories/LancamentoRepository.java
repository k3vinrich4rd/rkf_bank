package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    // query para buscar todos os lançamentos de uma conta, seja ela de origem ou destino, ordenados pela data e hora do lançamento em ordem decrescente
    List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);
}