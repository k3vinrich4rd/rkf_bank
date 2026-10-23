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

// Uma linha do extrato (tb_lancamento). E gravado na MESMA transacao que muda o saldo: se o lancamento falhar,
// o saldo volta junto (rollback). Lancamento nao se edita nem se apaga: e o registro do que aconteceu.
@Entity
@Table(name = "tb_lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nome do enum em VARCHAR (ver o comentario igual em Conta): ordem do enum pode mudar sem estragar o historico.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private TipoLancamentoEnum tipoLancamentoEnum;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // Para que lado o dinheiro andou: deposito so tem destino, saque so tem origem, transferencia tem os dois.
    // Sao ids soltos (UUID), e nao @ManyToOne para Conta: o extrato so precisa do id, e assim ler um lancamento
    // nao carrega duas contas inteiras do banco. Os indices idx_lancamento_origem/destino (V4) deixam o extrato rapido.
    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private String descricao;

    @Column(nullable = false)
    private Instant dataHora;

    // @PrePersist roda logo antes do INSERT: a data vem do servidor, nunca do cliente da API (nao da para forjar
    // a data de uma movimentacao). Instant = instante em UTC; a tela converte para o fuso de quem ve.
    @PrePersist
    void aoSalvar() {
        // Momento exato do INSERT, em UTC.
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