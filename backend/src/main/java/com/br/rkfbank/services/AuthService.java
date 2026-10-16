package com.br.rkfbank.services;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.repositories.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Hash de uma senha que ninguem tem. Usado quando o CPF nao existe, para o login gastar o mesmo tempo
    // do BCrypt nos dois casos; senao a demora da resposta revelaria quais CPFs tem cadastro.
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        // Acima de 72 bytes o BCrypt nao confere; nenhuma senha cadastrada passa desse limite.
        boolean senhaNoLimite = request.senha().getBytes(StandardCharsets.UTF_8).length <= ClienteService.MAX_BYTES_SENHA;

        Optional<Usuario> encontrado = usuarioRepository.findByLogin(cpf);
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = senhaNoLimite && passwordEncoder.matches(request.senha(), hash);

        if (encontrado.isEmpty() || !senhaConfere) {
            // CPF fica fora do log (dado pessoal); a mensagem e a mesma para os dois casos.
            log.warn("Login recusado: credenciais invalidas");
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        TokenService.TokenGerado token = tokenService.gerar(usuario);
        UUID clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        log.info("Usuario {} autenticado (perfil={})", usuario.getId(), usuario.getPerfil());
        return new LoginResponseDto(token.token(), "Bearer", token.expiraEmSegundos(), clienteId, usuario.getPerfil());
    }
}
