package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Conta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {
    // Verifica unicidade do numero da conta.
    boolean existsByNumeroConta(String numeroConta);
    // Contas do cliente logado (GET /contas para o perfil CLIENTE).
    List<Conta> findByClienteId(UUID clienteId);
    Page<Conta> findByClienteId(UUID clienteId, Pageable pageable);
}
