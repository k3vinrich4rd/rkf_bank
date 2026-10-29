package com.br.rkfbank.autenticacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.autenticacao.dto.request.LoginRequestDto;
import com.br.rkfbank.autenticacao.dto.request.TrocarSenhaRequestDto;
import com.br.rkfbank.autenticacao.dto.response.LoginResponseDto;
import com.br.rkfbank.clientes.Cliente;
import com.br.rkfbank.compartilhado.erros.CredenciaisInvalidasException;
import com.br.rkfbank.compartilhado.erros.MuitasTentativasException;
import com.br.rkfbank.compartilhado.erros.RegraNegocioException;
import com.br.rkfbank.compartilhado.erros.RequisicaoInvalidaException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    private static final String HASH_FALSO = "$2a$10$hash-da-senha-que-ninguem-tem";
    private static final String HASH_JOAO = "$2a$10$hash-da-senha-do-joao";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private LimitadorTentativasLogin limitador;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void setUp() {
        // O construtor gera o hash falso usado quando o CPF nao existe.
        when(passwordEncoder.encode(anyString())).thenReturn(HASH_FALSO);
        autenticacaoService = new AutenticacaoService(usuarioRepository, passwordEncoder, tokenService, limitador, refreshTokenService);
    }

    @Test
    void deveDevolverTokenQuandoCpfESenhaConferem() {
        UUID clienteId = UUID.randomUUID();
        Usuario joao = usuario(PerfilEnum.CLIENTE, clienteId);
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(joao));
        when(passwordEncoder.matches("senha-forte-1", HASH_JOAO)).thenReturn(true);
        when(tokenService.emitir(joao)).thenReturn(new TokenService.TokenEmitido("token.jwt.assinado", 3600));
        when(refreshTokenService.emitir(joao)).thenReturn(new RefreshTokenService.Emitido("token-de-renovacao", 604800));

        // CPF com mascara: o login normaliza para so digitos, igual ao cadastro.
        LoginResponseDto response = autenticacaoService.login(new LoginRequestDto("529.982.247-25", "senha-forte-1"));

        assertEquals("token.jwt.assinado", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals(3600, response.expiraEmSegundos());
        assertEquals(clienteId, response.clienteId());
        assertEquals(PerfilEnum.CLIENTE, response.perfil());
        assertEquals("token-de-renovacao", response.refreshToken());
        // Acertou: o contador de erros daquele CPF zera.
        verify(limitador).limpar("52998224725");
    }

    @Test
    void loginDoAdminVemSemClienteId() {
        Usuario admin = usuario(PerfilEnum.ADMIN, null);
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha-do-admin", HASH_JOAO)).thenReturn(true);
        when(tokenService.emitir(admin)).thenReturn(new TokenService.TokenEmitido("token", 3600));
        when(refreshTokenService.emitir(admin)).thenReturn(new RefreshTokenService.Emitido("token-de-renovacao", 604800));

        LoginResponseDto response = autenticacaoService.login(new LoginRequestDto("11144477735", "senha-do-admin"));

        assertNull(response.clienteId());
        assertEquals(PerfilEnum.ADMIN, response.perfil());
    }

    // ---------- troca da propria senha ----------

    @Test
    void trocaASenhaComASenhaAtualCertaESobeAVersaoDaCredencial() {
        Usuario joao = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());
        when(usuarioRepository.findById(joao.getId())).thenReturn(Optional.of(joao));
        when(passwordEncoder.matches("senha-forte-1", HASH_JOAO)).thenReturn(true);
        when(passwordEncoder.matches("senha-nova-123", HASH_JOAO)).thenReturn(false);
        when(passwordEncoder.encode("senha-nova-123")).thenReturn("$2a$10$hash-novo");

        autenticacaoService.trocarSenha(logado(joao), new TrocarSenhaRequestDto("senha-forte-1", "senha-nova-123"));

        assertEquals("$2a$10$hash-novo", joao.getSenhaHash());
        assertEquals(1, joao.getVersaoCredencial());
        verify(limitador).limpar("52998224725");
    }

    @Test
    void senhaAtualErradaE422EContaNoLimitador() {
        Usuario joao = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());
        when(usuarioRepository.findById(joao.getId())).thenReturn(Optional.of(joao));
        when(passwordEncoder.matches("chute", HASH_JOAO)).thenReturn(false);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> autenticacaoService.trocarSenha(logado(joao), new TrocarSenhaRequestDto("chute", "senha-nova-123")));

        assertEquals("Senha atual incorreta", ex.getMessage());
        assertEquals(HASH_JOAO, joao.getSenhaHash());
        verify(limitador).registrarFalha("52998224725");
    }

    @Test
    void novaSenhaIgualAAtualE422() {
        Usuario joao = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());
        when(usuarioRepository.findById(joao.getId())).thenReturn(Optional.of(joao));
        when(passwordEncoder.matches("senha-forte-1", HASH_JOAO)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> autenticacaoService.trocarSenha(logado(joao), new TrocarSenhaRequestDto("senha-forte-1", "senha-forte-1")));

        assertEquals("A nova senha precisa ser diferente da atual", ex.getMessage());
        assertEquals(0, joao.getVersaoCredencial());
    }

    @Test
    void trocaDeSenhaBloqueadaPeloLimitadorE429() {
        Usuario joao = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());
        when(usuarioRepository.findById(joao.getId())).thenReturn(Optional.of(joao));
        doThrow(new MuitasTentativasException(300)).when(limitador).conferir("52998224725");

        assertThrows(MuitasTentativasException.class,
                () -> autenticacaoService.trocarSenha(logado(joao), new TrocarSenhaRequestDto("senha-forte-1", "senha-nova-123")));
        verify(passwordEncoder, never()).matches(eq("senha-forte-1"), anyString());
    }

    @Test
    void novaSenhaAcimaDe72BytesE400AntesDeTudo() {
        Usuario joao = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());

        assertThrows(RequisicaoInvalidaException.class,
                () -> autenticacaoService.trocarSenha(logado(joao), new TrocarSenhaRequestDto("senha-forte-1", "ç".repeat(40))));
        verifyNoInteractions(usuarioRepository);
    }

    private static UsuarioLogado logado(Usuario usuario) {
        return new UsuarioLogado(usuario.getId(), usuario.getCliente() != null ? usuario.getCliente().getId() : null, usuario.getPerfil());
    }

    @Test
    void usuarioDesativadoNaoEntraMesmoComASenhaCerta() {
        Usuario desativado = usuario(PerfilEnum.CLIENTE, UUID.randomUUID());
        desativado.setAtivo(false);
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(desativado));
        when(passwordEncoder.matches("senha-forte-1", HASH_JOAO)).thenReturn(true);

        CredenciaisInvalidasException ex = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequestDto("52998224725", "senha-forte-1")));

        // Mesma mensagem de senha errada: de fora nao da para saber que a conta existe e foi desativada.
        assertEquals("CPF ou senha inválidos", ex.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void deveRecusarSenhaErrada() {
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())));
        when(passwordEncoder.matches("senha-errada", HASH_JOAO)).thenReturn(false);

        CredenciaisInvalidasException ex = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequestDto("52998224725", "senha-errada")));

        assertEquals("CPF ou senha inválidos", ex.getMessage());
        verifyNoInteractions(tokenService);
        verify(limitador).registrarFalha("52998224725");
    }

    @Test
    void cpfBloqueadoPorExcessoDeErrosDa429SemConferirASenha() {
        doThrow(new MuitasTentativasException(600)).when(limitador).conferir("52998224725");

        MuitasTentativasException ex = assertThrows(MuitasTentativasException.class,
                () -> autenticacaoService.login(new LoginRequestDto("529.982.247-25", "senha-forte-1")));

        assertEquals(600, ex.getSegundosParaLiberar());
        // Bloqueado nao gasta BCrypt nem descobre se a senha estava certa.
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(usuarioRepository, tokenService);
    }

    @Test
    void cpfInexistenteDaAMesmaRespostaEAindaRodaOBcrypt() {
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.empty());

        CredenciaisInvalidasException ex = assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequestDto("11144477735", "qualquer-senha")));

        assertEquals("CPF ou senha inválidos", ex.getMessage());
        // Confere contra o hash falso para gastar o mesmo tempo de um CPF que existe.
        verify(passwordEncoder).matches("qualquer-senha", HASH_FALSO);
        verifyNoInteractions(tokenService);
    }

    @Test
    void senhaAcimaDe72BytesNuncaConfere() {
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())));
        String senhaLonga = "ç".repeat(40);

        assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequestDto("52998224725", senhaLonga)));

        verify(passwordEncoder, never()).matches(eq(senhaLonga), anyString());
    }

    private Usuario usuario(PerfilEnum perfil, UUID clienteId) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setLogin("52998224725");
        usuario.setSenhaHash(HASH_JOAO);
        usuario.setPerfil(perfil);
        if (clienteId != null) {
            Cliente cliente = new Cliente();
            ReflectionTestUtils.setField(cliente, "id", clienteId);
            usuario.setCliente(cliente);
        }
        return usuario;
    }
}
