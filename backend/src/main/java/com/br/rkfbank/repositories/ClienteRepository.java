package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Acesso a tb_cliente. existsBy... gera "SELECT 1 ... LIMIT 1": mais barato que buscar o cliente inteiro so para
// saber se existe.
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    // Verifica se CPF ja existe para regra de unicidade.
    boolean existsByCpf(String cpf);
    // Verifica se email ja existe para regra de unicidade.
    boolean existsByEmail(String email);
    // LGPD: encerrados antes do limite (fim da retencao) e ainda nao anonimizados. Usa o idx_cliente_encerrado_em (V9).
    List<Cliente> findByEncerradoEmBeforeAndAnonimizadoEmIsNull(Instant limite);
}