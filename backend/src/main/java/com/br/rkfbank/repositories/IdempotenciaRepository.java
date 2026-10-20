package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.RegistroIdempotencia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotenciaRepository extends JpaRepository<RegistroIdempotencia, UUID> {
    // A chave so vale dentro do usuario que a enviou.
    Optional<RegistroIdempotencia> findByUsuarioIdAndChave(UUID usuarioId, String chave);
}
