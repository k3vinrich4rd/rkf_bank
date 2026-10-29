package com.br.rkfbank.autenticacao;

import com.br.rkfbank.autenticacao.dto.request.LoginRequestDto;
import com.br.rkfbank.autenticacao.dto.request.TrocarSenhaRequestDto;
import com.br.rkfbank.autenticacao.dto.response.LoginResponseDto;
import com.br.rkfbank.clientes.ClienteService;
import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import com.br.rkfbank.compartilhado.erros.CredenciaisInvalidasException;
import com.br.rkfbank.compartilhado.erros.RegraNegocioException;
import com.br.rkfbank.compartilhado.erros.SessaoExpiradaException;
import com.br.rkfbank.compartilhado.erros.UsuarioNaoEncontradoException;
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
    private final RefreshTokenService refreshTokenService;

    // Hash de uma senha que ninguem tem. Usado quando o CPF nao existe, para o login gastar o mesmo tempo
    // do BCrypt nos dois casos; senao a demora da resposta revelaria quais CPFs tem cadastro.
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       LimitadorTentativasLogin limitador, RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limitador = limitador;
        this.refreshTokenService = refreshTokenService;
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // Escreve so o token de renovacao (tb_refresh_token); o resto do login le o banco.
    @Transactional
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
        log.info("Usuario {} autenticado (perfil={})", usuario.getId(), usuario.getPerfil());
        return sessao(usuario);
    }

    // POST /auth/refresh: troca o token de renovacao por um par novo (o usado e revogado). Sem senha.
    // noRollbackFor: no 401 de reuso, a revogacao de todas as sessoes TEM que ser gravada mesmo com a excecao.
    @Transactional(noRollbackFor = SessaoExpiradaException.class)
    public LoginResponseDto renovar(String refreshToken) {
        Usuario usuario = refreshTokenService.consumir(refreshToken);
        log.info("Sessao do usuario {} renovada", usuario.getId());
        return sessao(usuario);
    }

    // POST /auth/logout: o token de renovacao deixa de valer. O de acesso ainda vale ate vencer (no maximo 15 min),
    // mas o front ja o apagou do cookie.
    @Transactional
    public void sair(String refreshToken) {
        refreshTokenService.revogar(refreshToken);
    }

    private LoginResponseDto sessao(Usuario usuario) {
        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        RefreshTokenService.Emitido renovacao = refreshTokenService.emitir(usuario);
        UUID clienteId = usuario.getCliente() != null ? usuario.getCliente().getId() : null;
        return new LoginResponseDto(token.token(), "Bearer", token.expiraEmSegundos(), clienteId, usuario.getPerfil(),
                usuario.isDeveTrocarSenha(), renovacao.valor(), renovacao.expiraEmSegundos());
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
            throw new RegraNegocioException(CodigoErroEnum.SENHA_ATUAL_INCORRETA);
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraNegocioException(CodigoErroEnum.SENHA_REPETIDA);
        }

        limitador.limpar(usuario.getLogin());
        usuario.trocarSenha(passwordEncoder.encode(request.novaSenha()));
        log.info("Usuario {} trocou a propria senha; tokens anteriores revogados", usuario.getId());
    }
}
