package com.br.rkfbank.services;

import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.repositories.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LogManager.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
        log.info("Usuario admin {} criado a partir do .env", salvo.getId());
    }
}
