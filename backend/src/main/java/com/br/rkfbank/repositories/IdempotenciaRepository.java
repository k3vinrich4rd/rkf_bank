package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.RegistroIdempotencia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// Chaves de idempotencia ja usadas. A protecao final contra duas chamadas simultaneas e o UNIQUE
// (usuario_id, chave) do banco (V6), nao este SELECT: entre o SELECT e o INSERT outra thread pode passar.
public interface IdempotenciaRepository extends JpaRepository<RegistroIdempotencia, UUID> {
    // A chave so vale dentro do usuario que a enviou.
    Optional<RegistroIdempotencia> findByUsuarioIdAndChave(UUID usuarioId, String chave);
}
