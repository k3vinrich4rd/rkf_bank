package com.br.rkfbank.entities;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Conta bancaria (tb_conta). Pertence a um cliente e guarda o saldo atual; o historico fica em tb_lancamento.
// Conta nunca e apagada: encerrar e "ativa = false", porque o extrato precisa continuar existindo.
@Entity
@Table(name = "tb_conta", uniqueConstraints = {
    @UniqueConstraint(name = "uk_conta_numero", columnNames = "numero_conta")
})
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_conta", nullable = false)
    private String numeroConta;

    @Column(nullable = false, length = 4)
    private String agencia;

    // Grava o NOME do enum ("CORRENTE") em VARCHAR, e nao a posicao (0, 1): incluir um tipo novo no meio do enum
    // nao muda o significado das contas antigas. VARCHAR em vez do ENUM nativo do MySQL: o H2 dos testes e
    // outros bancos entendem, e um tipo novo nao exige ALTER TABLE (o CHECK da migracao e que limita os valores).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoContaEnum tipoContaEnum;

    // DECIMAL(19,2): dinheiro exato com 2 casas. Nunca FLOAT/DOUBLE, que nao representam 0,10 exatamente.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false)
    private boolean ativa;

    // Lock otimista: todo UPDATE vira "... SET versao = versao + 1 WHERE id = ? AND versao = ?". Se duas
    // operacoes leram a mesma versao, a segunda nao acha a linha, o Hibernate lanca
    // ObjectOptimisticLockingFailureException e o handler devolve 409. Sem isso, uma escrita apagaria a outra
    // (lost update) e o saldo ficaria errado. O Hibernate controla o campo sozinho: nunca altere a mao.
    @Version
    private Long versao;

    // Dono da conta. ManyToOne: um cliente pode ter varias contas (corrente e poupanca). optional = false e a FK
    // fk_conta_cliente garantem que nao existe conta sem cliente. E daqui que sai a regra de dono no ContaService.
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public UUID getId() { return id; }
    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }
    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public TipoContaEnum getTipoConta() { return tipoContaEnum; }
    public void setTipoConta(TipoContaEnum tipoContaEnum) { this.tipoContaEnum = tipoContaEnum; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}