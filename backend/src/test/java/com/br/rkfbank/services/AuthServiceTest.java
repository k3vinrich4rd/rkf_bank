package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.repositories.UsuarioRepository;
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
class AuthServiceTest {

    private static final String HASH_FALSO = "$2a$10$hash-da-senha-que-ninguem-tem";
    private static final String HASH_JOAO = "$2a$10$hash-da-senha-do-joao";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // O construtor gera o hash falso usado quando o CPF nao existe.
        when(passwordEncoder.encode(anyString())).thenReturn(HASH_FALSO);
        authService = new AuthService(usuarioRepository, passwordEncoder, tokenService);
    }

    @Test
    void deveDevolverTokenQuandoCpfESenhaConferem() {
        UUID clienteId = UUID.randomUUID();
        Usuario joao = usuario(PerfilEnum.CLIENTE, clienteId);
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(joao));
        when(passwordEncoder.matches("senha-forte-1", HASH_JOAO)).thenReturn(true);
        when(tokenService.gerar(joao)).thenReturn(new TokenService.TokenGerado("token.jwt.assinado", 3600));

        // CPF com mascara: o login normaliza para so digitos, igual ao cadastro.
        LoginResponseDto response = authService.login(new LoginRequestDto("529.982.247-25", "senha-forte-1"));

        assertEquals("token.jwt.assinado", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals(3600, response.expiraEmSegundos());
        assertEquals(clienteId, response.clienteId());
        assertEquals(PerfilEnum.CLIENTE, response.perfil());
    }

    @Test
    void loginDoAdminVemSemClienteId() {
        Usuario admin = usuario(PerfilEnum.ADMIN, null);
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha-do-admin", HASH_JOAO)).thenReturn(true);
        when(tokenService.gerar(admin)).thenReturn(new TokenService.TokenGerado("token", 3600));

        LoginResponseDto response = authService.login(new LoginRequestDto("11144477735", "senha-do-admin"));

        assertNull(response.clienteId());
        assertEquals(PerfilEnum.ADMIN, response.perfil());
    }

    @Test
    void deveRecusarSenhaErrada() {
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())));
        when(passwordEncoder.matches("senha-errada", HASH_JOAO)).thenReturn(false);

        CredenciaisInvalidasException ex = assertThrows(CredenciaisInvalidasException.class,
                () -> authService.login(new LoginRequestDto("52998224725", "senha-errada")));

        assertEquals("CPF ou senha invalidos", ex.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void cpfInexistenteDaAMesmaRespostaEAindaRodaOBcrypt() {
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.empty());

        CredenciaisInvalidasException ex = assertThrows(CredenciaisInvalidasException.class,
                () -> authService.login(new LoginRequestDto("11144477735", "qualquer-senha")));

        assertEquals("CPF ou senha invalidos", ex.getMessage());
        // Confere contra o hash falso para gastar o mesmo tempo de um CPF que existe.
        verify(passwordEncoder).matches("qualquer-senha", HASH_FALSO);
        verifyNoInteractions(tokenService);
    }

    @Test
    void senhaAcimaDe72BytesNuncaConfere() {
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(usuario(PerfilEnum.CLIENTE, UUID.randomUUID())));
        String senhaLonga = "ç".repeat(40);

        assertThrows(CredenciaisInvalidasException.class,
                () -> authService.login(new LoginRequestDto("52998224725", senhaLonga)));

        verify(passwordEncoder, never()).matches(eq(senhaLonga), anyString());
    }

    private Usuario usuario(PerfilEnum perfil, UUID clienteId) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
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
