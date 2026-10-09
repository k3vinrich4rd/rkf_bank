package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    // Verifica se CPF ja existe para regra de unicidade.
    boolean existsByCpf(String cpf);
    // Verifica se email ja existe para regra de unicidade.
    boolean existsByEmail(String email);
}