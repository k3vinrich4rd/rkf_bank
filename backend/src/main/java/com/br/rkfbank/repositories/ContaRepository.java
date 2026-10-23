package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Conta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA: so a interface; o Spring gera a implementacao na subida. Metodos herdados do JpaRepository
// (findById, save, findAll...) cobrem o basico, e os declarados aqui sao "query methods": o SQL e montado a
// partir do NOME do metodo (existsByNumeroConta -> SELECT ... WHERE numero_conta = ? LIMIT 1).
public interface ContaRepository extends JpaRepository<Conta, UUID> {
    // Verifica unicidade do numero da conta.
    boolean existsByNumeroConta(String numeroConta);
    // Contas do cliente logado (GET /contas para o perfil CLIENTE).
    List<Conta> findByClienteId(UUID clienteId);
    Page<Conta> findByClienteId(UUID clienteId, Pageable pageable);
}
