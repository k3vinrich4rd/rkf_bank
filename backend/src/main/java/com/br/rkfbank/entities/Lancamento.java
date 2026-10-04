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

@Entity
@Table(name = "tb_lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamentoEnum tipoLancamentoEnum;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private String descricao;

    @Column(nullable = false)
    private Instant dataHora;

    @PrePersist
    void aoSalvar() {
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