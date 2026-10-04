package com.br.rkfbank.repositories;


import com.br.rkfbank.entities.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {
    // query para verificar se o número da conta já existe no banco de dados
    boolean existsByNumeroConta(String numeroConta);
}