package com.br.rkfbank.contas.comprovante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

// Um evento esperando para sair (V16). Gravado na transacao do dinheiro e publicado depois pelo PublicadorOutbox.
@Entity
@Table(name = "tb_outbox")
public class EventoOutbox {

    // Com 5 falhas seguidas o evento para de ser tentado (fica como DLQ, para o ADMIN olhar no banco).
    public static final int MAX_TENTATIVAS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "payload", nullable = false, length = 1000)
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    protected EventoOutbox() {
    }

    public EventoOutbox(String tipo, String payload) {
        this.tipo = tipo;
        this.payload = payload;
    }

    @PrePersist
    void aoSalvar() {
        this.criadoEm = Instant.now();
    }

    public void marcarPublicado(Instant quando) {
        this.publicadoEm = quando;
        this.ultimoErro = null;
    }

    // So a classe do erro (nunca a mensagem, que pode trazer dado de quem chamou) e cortada no tamanho da coluna.
    public void registrarFalha(Exception erro) {
        this.tentativas++;
        String nome = erro.getClass().getSimpleName();
        this.ultimoErro = nome.length() > 255 ? nome.substring(0, 255) : nome;
    }

    public boolean isPublicado() {
        return publicadoEm != null;
    }

    public boolean desistiu() {
        return !isPublicado() && tentativas >= MAX_TENTATIVAS;
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public int getTentativas() {
        return tentativas;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    // Identidade pelo id do banco, como as outras entidades (IdentidadeEntidadesTest).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoOutbox outro)) return false;
        return getId() != null && getId().equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return EventoOutbox.class.hashCode();
    }
}
