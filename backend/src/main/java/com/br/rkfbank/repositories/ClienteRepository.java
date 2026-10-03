package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    // queries para verificar se o CPF ou email já existem no banco de dados
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
}