package com.br.rkfbank.contas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Uma movimentacao ja feita com uma Idempotency-Key (V6). Repetir a chave nao movimenta de novo.
@Entity
@Table(name = "tb_idempotencia", uniqueConstraints = {
    @UniqueConstraint(name = "uk_idempotencia_usuario_chave", columnNames = {"usuario_id", "chave"})
})
public class RegistroIdempotencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Chave vale por usuario: a mesma chave em usuarios diferentes sao operacoes diferentes.
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 100)
    private String chave;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private TipoLancamentoEnum operacao;

    // SHA-256 (hex) da operacao: conta(s), valor e descricao. Nada de dado pessoal.
    @Column(name = "hash_requisicao", nullable = false, length = 64)
    private String hashRequisicao;

    // Quando a chave foi usada. Repeticao legitima acontece em segundos (retry de rede), entao chaves antigas podem
    // ser apagadas por uma limpeza periodica sem risco de cobrar duas vezes.
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @PrePersist
    void aoSalvar() {
        this.criadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public TipoLancamentoEnum getOperacao() { return operacao; }
    public void setOperacao(TipoLancamentoEnum operacao) { this.operacao = operacao; }
    public String getHashRequisicao() { return hashRequisicao; }
    public void setHashRequisicao(String hashRequisicao) { this.hashRequisicao = hashRequisicao; }
    public Instant getCriadoEm() { return criadoEm; }

    // Identidade pelo id do banco e hashCode fixo por classe: mesmo motivo explicado em Conta.equals.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroIdempotencia outro)) return false;
        return getId() != null && getId().equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return RegistroIdempotencia.class.hashCode();
    }
}
