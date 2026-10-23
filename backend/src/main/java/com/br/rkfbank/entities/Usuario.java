package com.br.rkfbank.entities;

import com.br.rkfbank.entities.enums.PerfilEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Credencial de acesso (tb_usuario, V5). Fica fora do Cliente para que dado cadastral e segredo nao andem juntos:
// quem le o cadastro nao carrega o hash da senha, e o ADMIN pode existir sem ser cliente (cliente_id nulo).
@Entity
@Table(name = "tb_usuario", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usuario_login", columnNames = "login"),
    @UniqueConstraint(name = "uk_usuario_cliente", columnNames = "cliente_id")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // CPF so com digitos.
    @Column(nullable = false, length = 11)
    private String login;

    // Hash BCrypt (60 caracteres). Nunca a senha em texto.
    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PerfilEnum perfil;

    // Desativado (V7): login falha e tokens ja emitidos dao 401. Um ADMIN liga e desliga pela rota /admin/usuarios.
    @Column(nullable = false)
    private boolean ativo = true;

    // Sobe a cada troca de senha (V8) e vai no token: token com versao antiga e recusado.
    @Column(name = "versao_credencial", nullable = false)
    private int versaoCredencial;

    // Senha temporaria (V14): o ADMIN redefiniu a senha, entao so a troca de senha e liberada ate a pessoa escolher outra.
    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha;

    // Nulo para o ADMIN, que nao e cliente.
    @OneToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    public UUID getId() { return id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public PerfilEnum getPerfil() { return perfil; }
    public void setPerfil(PerfilEnum perfil) { this.perfil = perfil; }
    public Cliente getCliente() { return cliente; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getVersaoCredencial() { return versaoCredencial; }
    public boolean isDeveTrocarSenha() { return deveTrocarSenha; }

    // Troca de senha de quem ja existe: novo hash e nova versao, para derrubar os tokens emitidos com a senha antiga.
    // Quem escolhe a propria senha deixa de ter senha temporaria.
    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
        this.versaoCredencial++;
        this.deveTrocarSenha = false;
    }

    // Senha que o ADMIN definiu e passou para a pessoa: ela entra, mas so pode trocar a senha (V14).
    public void redefinirSenhaTemporaria(String novoHash) {
        trocarSenha(novoHash);
        this.deveTrocarSenha = true;
    }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
