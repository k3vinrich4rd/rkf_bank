package com.br.rkfbank.autenticacao;

import com.br.rkfbank.clientes.Cliente;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// A parte do cadastro que e do modulo de autenticacao: o login (CPF) e a senha do cliente. O ClienteService chama
// aqui em vez de mexer no UsuarioRepository e no BCrypt, que sao deste modulo. Sem @Transactional proprio: roda na
// transacao do cadastro (endereco, cliente e credencial juntos; falhou um, nenhum fica).
@Service
public class CredencialService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CredencialService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // O CPF ja e login de alguem (inclusive de um admin que nao e cliente)?
    public boolean loginEmUso(String login) {
        return usuarioRepository.existsByLogin(login);
    }

    // Credencial CLIENTE do cadastro novo. So o hash BCrypt vai para o banco; a senha em texto termina aqui.
    // saveAndFlush: uma constraint unica (cadastro concorrente) estoura agora, dentro do try do cadastro.
    public void criarParaCliente(Cliente cliente, String login, String senha) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(PerfilEnum.CLIENTE);
        usuario.setCliente(cliente);
        usuarioRepository.saveAndFlush(usuario);
    }
}
