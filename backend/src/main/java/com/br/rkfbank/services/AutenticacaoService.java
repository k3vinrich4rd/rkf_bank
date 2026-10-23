package com.br.rkfbank.services;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.request.auth.TrocarSenhaRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.UsuarioNaoEncontradoException;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.security.LimitadorTentativasLogin;
import com.br.rkfbank.security.UsuarioLogado;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Autenticacao: confere CPF + senha e emite o token. Tres cuidados de seguranca de banco:
//  1. mesma resposta e mesmo tempo para CPF inexistente e senha errada (nao revela quem e cliente);
//  2. limite de tentativas por CPF (LimitadorTentativasLogin) contra chute de senha;
//  3. senha so comparada com o hash BCrypt (passwordEncoder.matches); a senha em texto nunca e guardada.
@Service
public class AutenticacaoService {

    private static final Logger log = LogManager.getLogger(AutenticacaoService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimitadorTentativasLogin limitador;

    // Hash de uma senha que ninguem tem. Usado quando o CPF nao existe, para o login gastar o mesmo tempo
    // do BCrypt nos dois casos; senao a demora da resposta revelaria quais CPFs tem cadastro.
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       LimitadorTentativasLogin limitador) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limitador = limitador;
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // readOnly = true: o login so le o banco; o Hibernate pula a checagem de mudancas e o banco pode otimizar.
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        // Acima de 72 bytes o BCrypt nao confere; nenhuma senha cadastrada passa desse limite.
        boolean senhaNoLimite = request.senha().getBytes(StandardCharsets.UTF_8).length <= ClienteService.MAX_BYTES_SENHA;
        // Forca bruta: CPF que errou demais recebe 429 antes de qualquer conferencia de senha.
        limitador.conferir(cpf);

        Optional<Usuario> encontrado = usuarioRepository.findByLogin(cpf);
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = senhaNoLimite && passwordEncoder.matches(request.senha(), hash);

        if (encontrado.isEmpty() || !senhaConfere) {
            // CPF fica fora do log (dado pessoal); a mensagem e a mesma para os dois casos.
            log.warn("Login recusado: credenciais invalidas");
            limitador.registrarFalha(cpf);
            throw new CredenciaisInvalidasException();
        }

        limitador.limpar(cpf);
        Usuario usuario = encontrado.get();
        if (!usuario.isAtivo()) {
            // Senha certa, mas o acesso foi desativado por um ADMIN. Mesma resposta de credencial invalida:
            // quem tenta de fora nao descobre que a conta existe.
            log.warn("Login recusado: usuario {} desativado", usuario.getId());
            throw new CredenciaisInvalidasException();
        }
        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        UUID clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        log.info("Usuario {} autenticado (perfil={})", usuario.getId(), usuario.getPerfil());
        return new LoginResponseDto(token.token(), "Bearer", token.expiraEmSegundos(), clienteId, usuario.getPerfil());
    }

    // Troca da propria senha. Exige a senha atual (um token roubado nao basta) e conta erro no mesmo limitador do
    // login: chutar a senha atual por aqui tambem leva a 429. Depois da troca, todo token antigo dele vira 401.
    @Transactional
    public void trocarSenha(UsuarioLogado quem, TrocarSenhaRequestDto request) {
        ClienteService.garantirLimiteDoBcrypt("novaSenha", request.novaSenha());
        Usuario usuario = usuarioRepository.findById(quem.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
        limitador.conferir(usuario.getLogin());

        boolean senhaAtualNoLimite = request.senhaAtual().getBytes(StandardCharsets.UTF_8).length <= ClienteService.MAX_BYTES_SENHA;
        if (!senhaAtualNoLimite || !passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            limitador.registrarFalha(usuario.getLogin());
            log.warn("Troca de senha recusada: senha atual incorreta (usuario {})", usuario.getId());
            throw new RegraNegocioException("Senha atual incorreta");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha precisa ser diferente da atual");
        }

        limitador.limpar(usuario.getLogin());
        usuario.trocarSenha(passwordEncoder.encode(request.novaSenha()));
        log.info("Usuario {} trocou a propria senha; tokens anteriores revogados", usuario.getId());
    }
}
