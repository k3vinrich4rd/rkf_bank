package com.br.rkfbank.services;

import com.br.rkfbank.dto.response.usuario.UsuarioAdminResponseDto;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.UsuarioNaoEncontradoException;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Gestao de usuarios pelo ADMIN: listar, promover/rebaixar e desativar/reativar. Usuario nunca e apagado (o historico
// aponta para ele); "remover" alguem = desativar. Toda mudanca vale na proxima requisicao: o ConversorTokenUsuario
// recusa (401) o token emitido antes da mudanca, e a pessoa entra de novo ja com o perfil novo.
@Service
public class GestaoUsuarioService {

    private static final Logger log = LogManager.getLogger(GestaoUsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public GestaoUsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Todos os usuarios para o ADMIN, admins primeiro. Resposta minima: CPF mascarado, nunca hash.
    @Transactional(readOnly = true)
    public List<UsuarioAdminResponseDto> listar() {
        return usuarioRepository.findAllByOrderByPerfilAscLoginAsc().stream().map(GestaoUsuarioService::paraResponse).toList();
    }

    // Promove ou rebaixa. So muda o banco; o token antigo da pessoa cai sozinho na proxima requisicao, porque o
    // ConversorTokenUsuario ve que o perfil do token nao bate mais com o do banco.
    @Transactional
    public UsuarioAdminResponseDto alterarPerfil(UUID usuarioId, PerfilEnum perfil, UsuarioLogado quem) {
        Usuario alvo = buscarOutro(usuarioId, quem);
        if (perfil == PerfilEnum.CLIENTE && alvo.getCliente() == null) {
            // Admin criado pelo .env nao tem cadastro: como CLIENTE ele nao teria conta nem dados. Desative em vez disso.
            throw new RegraNegocioException(CodigoErroEnum.PERFIL_CLIENTE_SEM_CADASTRO);
        }
        if (alvo.getPerfil() != perfil) {
            log.info("Usuario {} mudou o perfil do usuario {} de {} para {}", quem.usuarioId(), alvo.getId(), alvo.getPerfil(), perfil);
            alvo.setPerfil(perfil);
        }
        return paraResponse(alvo);
    }

    // Desativa ou reativa o login. Desativar e o "remover" de um banco: o usuario some do acesso, mas o historico
    // (movimentacoes, chaves de idempotencia) continua apontando para ele.
    @Transactional
    public UsuarioAdminResponseDto alterarAcesso(UUID usuarioId, boolean ativo, UsuarioLogado quem) {
        Usuario alvo = buscarOutro(usuarioId, quem);
        if (ativo && alvo.getCliente() != null && alvo.getCliente().isEncerrado()) {
            // LGPD: o titular encerrou o cadastro. Reativar seria reabrir uma relacao que ele pediu para acabar.
            log.warn("Usuario {} tentou reativar o usuario {}, cujo cadastro foi encerrado", quem.usuarioId(), alvo.getId());
            throw new RegraNegocioException(CodigoErroEnum.CADASTRO_ENCERRADO);
        }
        if (alvo.isAtivo() != ativo) {
            log.info("Usuario {} {} o acesso do usuario {}", quem.usuarioId(), ativo ? "reativou" : "desativou", alvo.getId());
            alvo.setAtivo(ativo);
        }
        return paraResponse(alvo);
    }

    // Para quem esqueceu a senha: o ADMIN define uma nova, que passa a pessoa por canal privado. Os tokens dela caem
    // na hora. A propria senha o ADMIN troca por PATCH /usuarios/me/senha (com a senha atual), nunca por aqui.
    @Transactional
    public void redefinirSenha(UUID usuarioId, String novaSenha, UsuarioLogado quem) {
        if (usuarioId.equals(quem.usuarioId())) {
            throw new RegraNegocioException(CodigoErroEnum.PROPRIA_SENHA_PELA_GESTAO);
        }
        ClienteService.garantirLimiteDoBcrypt("novaSenha", novaSenha);
        Usuario alvo = buscarOutro(usuarioId, quem);
        // Temporaria: no proximo login a pessoa so consegue trocar a senha (o ADMIN conhece esta).
        alvo.redefinirSenhaTemporaria(passwordEncoder.encode(novaSenha));
        log.info("Usuario {} redefiniu a senha do usuario {}; tokens anteriores revogados e troca obrigatoria", quem.usuarioId(), alvo.getId());
    }

    // Ninguem mexe no proprio perfil ou acesso. Alem de evitar o admin se trancar para fora, garante que sempre sobra
    // pelo menos um ADMIN ativo: quem chama e um ADMIN ativo e nunca e o alvo.
    private Usuario buscarOutro(UUID usuarioId, UsuarioLogado quem) {
        if (usuarioId.equals(quem.usuarioId())) {
            log.warn("Usuario {} tentou alterar o proprio perfil ou acesso", quem.usuarioId());
            throw new RegraNegocioException(CodigoErroEnum.PROPRIO_ACESSO);
        }
        return usuarioRepository.findById(usuarioId).orElseThrow(() -> {
            log.warn("Usuario {} nao encontrado na gestao de usuarios", usuarioId);
            return new UsuarioNaoEncontradoException();
        });
    }

    private static UsuarioAdminResponseDto paraResponse(Usuario usuario) {
        return new UsuarioAdminResponseDto(
                usuario.getId(),
                mascararCpf(usuario.getLogin()),
                usuario.getCliente() != null ? usuario.getCliente().getNomeCompleto() : null,
                usuario.getPerfil(),
                usuario.isAtivo(),
                usuario.getCliente() != null ? usuario.getCliente().getId() : null);
    }

    // 12345678909 -> ***.456.789-** (a regra mora no ClienteService: um formato so de CPF mascarado na API inteira).
    static String mascararCpf(String cpf) {
        return ClienteService.mascararCpf(cpf);
    }
}
