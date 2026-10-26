package com.br.rkfbank.services;

import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.repositories.ClienteRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Cria credenciais que dependem do ambiente (.env): o primeiro ADMIN e o login das contas da equipe.
// Senha nunca em migracao ou seed: o repositorio e publico, e o que entra no Git fica no historico para sempre.
@Service
public class UsuarioService {

    private static final Logger log = LogManager.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clienteRepository = clienteRepository;
    }

    // Login da conta da equipe (seed): cria o usuario CLIENTE se faltar, ou troca o hash se EQUIPE_SENHA mudou.
    // Devolve false quando o cliente nao existe (o db/seed/equipe nao esta ligado neste ambiente).
    @Transactional
    public boolean garantirAcessoDaEquipe(UUID clienteId, String senha) {
        int bytesSenha = senha.getBytes(StandardCharsets.UTF_8).length;
        if (bytesSenha < 12 || bytesSenha > ClienteService.MAX_BYTES_SENHA) {
            throw new IllegalStateException("EQUIPE_SENHA deve ter entre 12 e " + ClienteService.MAX_BYTES_SENHA + " bytes");
        }
        Optional<Cliente> cliente = clienteRepository.findById(clienteId);
        if (cliente.isEmpty()) {
            log.debug("Cliente da equipe {} nao existe neste banco; login nao criado", clienteId);
            return false;
        }

        String login = cliente.get().getCpf();
        Optional<Usuario> existente = usuarioRepository.findByLogin(login);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.getCliente() == null || !usuario.getCliente().getId().equals(clienteId)) {
                // O CPF do seed foi usado por outro cadastro: nunca sequestra o login de alguem.
                log.warn("Login da conta da equipe {} pertence a outro usuario; nada alterado", clienteId);
                return false;
            }
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                usuario.trocarSenha(passwordEncoder.encode(senha));
                log.info("Senha da conta da equipe {} atualizada a partir do .env", clienteId);
            }
            return true;
        }

        Usuario novo = new Usuario();
        novo.setLogin(login);
        novo.setSenhaHash(passwordEncoder.encode(senha));
        novo.setPerfil(PerfilEnum.CLIENTE);
        novo.setCliente(cliente.get());
        usuarioRepository.save(novo);
        log.info("Login da conta da equipe {} criado a partir do .env", clienteId);
        return true;
    }

    // Idempotente: roda a cada subida e so cria o admin na primeira vez. Trocar ADMIN_SENHA depois nao altera o banco.
    @Transactional
    public void criarAdminSeNaoExistir(String cpf, String senha) {
        String login = cpf.replaceAll("\\D", "");
        if (login.length() != 11) {
            throw new IllegalStateException("ADMIN_CPF deve ter 11 digitos");
        }
        int bytesSenha = senha.getBytes(StandardCharsets.UTF_8).length;
        if (bytesSenha < 8 || bytesSenha > ClienteService.MAX_BYTES_SENHA) {
            throw new IllegalStateException("ADMIN_SENHA deve ter entre 8 e " + ClienteService.MAX_BYTES_SENHA + " bytes");
        }

        Optional<Usuario> existente = usuarioRepository.findByLogin(login);
        if (existente.isPresent()) {
            if (existente.get().getPerfil() != PerfilEnum.ADMIN) {
                // Nunca promove um cliente a admin em silencio.
                log.warn("ADMIN_CPF ja pertence a um cliente; admin nao criado");
            } else {
                log.info("Usuario admin ja existe; nada a fazer");
            }
            return;
        }

        Usuario admin = new Usuario();
        admin.setLogin(login);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPerfil(PerfilEnum.ADMIN);
        Usuario salvo = usuarioRepository.save(admin);
        log.info("Usuario admin {} criado a partir de ADMIN_CPF e ADMIN_SENHA (.env ou variaveis do ambiente)", salvo.getId());
    }
}
