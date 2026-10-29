package com.br.rkfbank.contas.comprovante;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<EventoOutbox, UUID> {

    // Lote do publicador: nao publicados que ainda tem tentativa, do mais antigo para o mais novo.
    List<EventoOutbox> findTop100ByPublicadoEmIsNullAndTentativasLessThanOrderByCriadoEmAsc(int maxTentativas);

    // Limpeza diaria: publicados ha mais de 30 dias ja cumpriram o papel. Os que desistiram (DLQ) ficam.
    @Modifying
    @Query("delete from EventoOutbox e where e.publicadoEm < :limite")
    int apagarPublicadosAntesDe(@Param("limite") Instant limite);
}
