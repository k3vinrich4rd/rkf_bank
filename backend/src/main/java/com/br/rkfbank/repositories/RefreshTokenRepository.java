package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Tokens de renovacao (V15). A busca e sempre pelo hash: o valor do token nunca chega ao banco.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByHashToken(String hashToken);

    // Reuso de um token ja revogado (roubo) ou pedido de "sair de todos os lugares": derruba todos do usuario.
    @Modifying
    @Query("update RefreshToken r set r.revogadoEm = :quando where r.usuarioId = :usuarioId and r.revogadoEm is null")
    int revogarTodosDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("quando") Instant quando);

    // Limpeza diaria: vencidos ha mais de 30 dias nao servem nem para investigar reuso.
    @Modifying
    @Query("delete from RefreshToken r where r.expiraEm < :limite")
    int apagarVencidosAntesDe(@Param("limite") Instant limite);
}
