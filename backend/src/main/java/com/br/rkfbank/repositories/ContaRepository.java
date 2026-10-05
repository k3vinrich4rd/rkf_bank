package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {
    // Verifica unicidade do numero da conta.
    boolean existsByNumeroConta(String numeroConta);
}