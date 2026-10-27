package com.br.rkfbank.entities;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.RegraNegocioException;
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

    // Dominio rico: a regra do dinheiro mora aqui, perto do saldo, e nao espalhada pelo service. Quem chama nao
    // consegue deixar o saldo negativo nem somar valor invalido, porque nao ha outro caminho (feito com TDD: ContaTest).
    public void depositar(BigDecimal valor) {
        exigirValorPositivo(valor);
        saldo = saldo.add(valor);
    }

    // Saque: confere o saldo ANTES de subtrair (422 se faltar). compareTo, e nao equals: 10.0 e 10.00 sao o mesmo
    // valor, mas equals os acha diferentes por causa da escala. Sacar exatamente o saldo e permitido.
    public void sacar(BigDecimal valor) {
        exigirValorPositivo(valor);
        if (saldo.compareTo(valor) < 0) {
            throw new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE);
        }
        saldo = saldo.subtract(valor);
    }

    // Transferencia: tudo ou nada. Confere destino e saldo antes de mexer em qualquer uma das duas contas.
    public void transferirPara(Conta destino, BigDecimal valor) {
        if (this.equals(destino)) {
            throw new RegraNegocioException(CodigoErroEnum.MESMA_CONTA);
        }
        sacar(valor);
        destino.depositar(valor);
    }

    private static void exigirValorPositivo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(CodigoErroEnum.VALOR_INVALIDO);
        }
    }

    // Identidade de entidade JPA = o id do banco (UUID gerado no persist). Antes do persist o id e null: so a
    // propria instancia e igual a si mesma (dois cadastros novos nunca "colidem"). O hashCode e fixo por classe
    // porque o id muda de null para o UUID no persist: se o hash dependesse dele, a entidade "sumiria" de um
    // HashSet/HashMap depois de salva. Usa getId() (e nao o campo) porque o proxy lazy do Hibernate e uma
    // subclasse que so responde pelo getter; por isso tambem instanceof (aceita o proxy) e nada de final.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conta outro)) return false;
        return getId() != null && getId().equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return Conta.class.hashCode();
    }
}
