package com.br.rkfbank.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Cliente do banco (tb_cliente): o cadastro da pessoa. CPF e e-mail sao unicos (uk_cliente_cpf, uk_cliente_email),
// e o banco garante isso mesmo que duas requisicoes passem juntas pela checagem do service.
// Credencial (senha) fica em Usuario; endereco, em Endereco. Cada tabela guarda um assunto.
@Entity
@Table(name = "tb_cliente", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cliente_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uk_cliente_email", columnNames = "email")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    // Endereco obrigatorio do cliente; cascade salva/atualiza junto.
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    // Contas do cliente. So PERSIST: salvar um cliente novo salva as contas novas junto, mas nada aqui APAGA conta.
    // Banco nao apaga conta (o extrato precisa sobreviver): encerra com ativa = false. Sem orphanRemoval, tirar a
    // conta da lista nao a remove; e a FK fk_conta_cliente impede excluir um cliente que ainda tem conta.
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.PERSIST)
    private List<Conta> contas = new ArrayList<>();

    // LGPD (V9): quando o titular encerrou o cadastro e quando os dados foram anonimizados. Nulos = cliente ativo.
    private Instant encerradoEm;

    private Instant anonimizadoEm;

    // LGPD (V10): qual versao dos termos o titular aceitou no cadastro e quando. Nulos = cadastro anterior a V10.
    private String termosVersao;

    private Instant termosAceitosEm;

    public UUID getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public List<Conta> getContas() {
        return contas;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    public boolean isEncerrado() {
        return encerradoEm != null;
    }

    public void encerrar(Instant quando) {
        this.encerradoEm = quando;
    }

    public Instant getAnonimizadoEm() {
        return anonimizadoEm;
    }

    public void marcarAnonimizado(Instant quando) {
        this.anonimizadoEm = quando;
    }

    public String getTermosVersao() {
        return termosVersao;
    }

    public Instant getTermosAceitosEm() {
        return termosAceitosEm;
    }

    public void registrarAceiteDosTermos(String versao, Instant quando) {
        this.termosVersao = versao;
        this.termosAceitosEm = quando;
    }

    // Identidade pelo id do banco e hashCode fixo por classe: mesmo motivo explicado em Conta.equals.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente outro)) return false;
        return getId() != null && getId().equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return Cliente.class.hashCode();
    }
}
