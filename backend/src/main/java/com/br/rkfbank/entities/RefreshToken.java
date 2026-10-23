package com.br.rkfbank.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

// Um token de renovacao emitido no login (V15). So o SHA-256 fica no banco; o valor em si so o cliente conhece.
@Entity
@Table(name = "tb_refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "hash_token", nullable = false, length = 64)
    private String hashToken;

    // A versao da senha na emissao: trocou a senha depois, este token nao renova mais.
    @Column(name = "versao_credencial", nullable = false)
    private int versaoCredencial;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    protected RefreshToken() {
    }

    public RefreshToken(UUID usuarioId, String hashToken, int versaoCredencial, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.hashToken = hashToken;
        this.versaoCredencial = versaoCredencial;
        this.expiraEm = expiraEm;
    }

    @PrePersist
    void aoSalvar() {
        this.criadoEm = Instant.now();
    }

    public boolean isRevogado() {
        return revogadoEm != null;
    }

    public void revogar(Instant quando) {
        if (revogadoEm == null) {
            this.revogadoEm = quando;
        }
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getHashToken() { return hashToken; }
    public int getVersaoCredencial() { return versaoCredencial; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getRevogadoEm() { return revogadoEm; }
}
