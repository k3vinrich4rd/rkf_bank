package com.br.rkfbank.entities;

import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Persistido como VARCHAR para manter flexibilidade do contrato da API.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private TipoLancamentoEnum tipoLancamentoEnum;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // Em deposito/saque apenas um lado pode estar preenchido; em transferencia ambos.
    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private String descricao;

    @Column(nullable = false)
    private Instant dataHora;

    @PrePersist
    void aoSalvar() {
        // Timestamp de criacao do lancamento no momento do insert.
        this.dataHora = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public TipoLancamentoEnum getTipoLancamentoEnum() {
        return tipoLancamentoEnum;
    }

    public void setTipoLancamentoEnum(TipoLancamentoEnum tipoLancamentoEnum) {
        this.tipoLancamentoEnum = tipoLancamentoEnum;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public UUID getContaOrigemId() {
        return contaOrigemId;
    }

    public void setContaOrigemId(UUID contaOrigemId) {
        this.contaOrigemId = contaOrigemId;
    }

    public UUID getContaDestinoId() {
        return contaDestinoId;
    }

    public void setContaDestinoId(UUID contaDestinoId) {
        this.contaDestinoId = contaDestinoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}