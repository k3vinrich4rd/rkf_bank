package com.br.rkfbank.contas;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Chaves de idempotencia ja usadas. A protecao final contra duas chamadas simultaneas e o UNIQUE
// (usuario_id, chave) do banco (V6), nao este SELECT: entre o SELECT e o INSERT outra thread pode passar.
public interface IdempotenciaRepository extends JpaRepository<RegistroIdempotencia, UUID> {
    // A chave so vale dentro do usuario que a enviou.
    Optional<RegistroIdempotencia> findByUsuarioIdAndChave(UUID usuarioId, String chave);

    // Chaves velhas nao servem para nada: repeticao de verdade (rede caiu, clique duplo) acontece em segundos.
    // Apaga em lote, sem carregar as entidades. Devolve quantas linhas sairam.
    @Modifying
    @Query("delete from RegistroIdempotencia r where r.criadoEm < :limite")
    int apagarCriadasAntesDe(@Param("limite") Instant limite);
}
