package com.br.rkfbank.autenticacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.clientes.Cliente;
import com.br.rkfbank.clientes.ClienteRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ClienteRepository clienteRepository;

    private static final UUID FELLIPE = UUID.fromString("5eed0000-0000-4000-8000-000000000012");
    private static final String SENHA_EQUIPE = "senha-da-equipe-de-teste";

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCriarAdminComSenhaEmHashESemCliente() {
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-do-admin")).thenReturn("$2a$10$hash-do-admin");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.criarAdminSeNaoExistir("111.444.777-35", "senha-do-admin");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario admin = captor.getValue();
        assertEquals("11144477735", admin.getLogin());
        assertEquals("$2a$10$hash-do-admin", admin.getSenhaHash());
        assertEquals(PerfilEnum.ADMIN, admin.getPerfil());
        assertNull(admin.getCliente());
    }

    @Test
    void naoDuplicaAdminQueJaExiste() {
        Usuario existente = new Usuario();
        existente.setPerfil(PerfilEnum.ADMIN);
        when(usuarioRepository.findByLogin("11144477735")).thenReturn(Optional.of(existente));

        usuarioService.criarAdminSeNaoExistir("11144477735", "senha-do-admin");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void naoPromoveClienteAAdmin() {
        Usuario cliente = new Usuario();
        cliente.setPerfil(PerfilEnum.CLIENTE);
        when(usuarioRepository.findByLogin("52998224725")).thenReturn(Optional.of(cliente));

        usuarioService.criarAdminSeNaoExistir("52998224725", "senha-do-admin");

        assertEquals(PerfilEnum.CLIENTE, cliente.getPerfil());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void recusaCpfSemOnzeDigitos() {
        assertThrows(IllegalStateException.class, () -> usuarioService.criarAdminSeNaoExistir("123", "senha-do-admin"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void recusaSenhaCurta() {
        assertThrows(IllegalStateException.class, () -> usuarioService.criarAdminSeNaoExistir("11144477735", "1234567"));
        verifyNoInteractions(usuarioRepository);
    }

    // ---------- acesso da equipe (EQUIPE_SENHA) ----------

    @Test
    void criaOLoginDaContaDaEquipeComOCpfDoCliente() {
        Cliente fellipe = cliente(FELLIPE, "12345678909");
        when(clienteRepository.findById(FELLIPE)).thenReturn(Optional.of(fellipe));
        when(usuarioRepository.findByLogin("12345678909")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(SENHA_EQUIPE)).thenReturn("$2a$10$hash-da-equipe");

        assertTrue(usuarioService.garantirAcessoDaEquipe(FELLIPE, SENHA_EQUIPE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("12345678909", captor.getValue().getLogin());
        assertEquals("$2a$10$hash-da-equipe", captor.getValue().getSenhaHash());
        assertEquals(PerfilEnum.CLIENTE, captor.getValue().getPerfil());
        assertEquals(fellipe, captor.getValue().getCliente());
    }

    @Test
    void trocaOHashQuandoAEquipeSenhaMuda() {
        Cliente fellipe = cliente(FELLIPE, "12345678909");
        Usuario existente = usuarioDo(fellipe, "$2a$10$hash-antigo");
        when(clienteRepository.findById(FELLIPE)).thenReturn(Optional.of(fellipe));
        when(usuarioRepository.findByLogin("12345678909")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches(SENHA_EQUIPE, "$2a$10$hash-antigo")).thenReturn(false);
        when(passwordEncoder.encode(SENHA_EQUIPE)).thenReturn("$2a$10$hash-novo");

        assertTrue(usuarioService.garantirAcessoDaEquipe(FELLIPE, SENHA_EQUIPE));

        assertEquals("$2a$10$hash-novo", existente.getSenhaHash());
    }

    @Test
    void naoMexeQuandoASenhaJaConfere() {
        Cliente fellipe = cliente(FELLIPE, "12345678909");
        Usuario existente = usuarioDo(fellipe, "$2a$10$hash-atual");
        when(clienteRepository.findById(FELLIPE)).thenReturn(Optional.of(fellipe));
        when(usuarioRepository.findByLogin("12345678909")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches(SENHA_EQUIPE, "$2a$10$hash-atual")).thenReturn(true);

        assertTrue(usuarioService.garantirAcessoDaEquipe(FELLIPE, SENHA_EQUIPE));

        assertEquals("$2a$10$hash-atual", existente.getSenhaHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void nuncaAssumeOLoginDeOutroCadastroComOMesmoCpf() {
        Cliente fellipe = cliente(FELLIPE, "12345678909");
        Usuario deOutro = usuarioDo(cliente(UUID.randomUUID(), "12345678909"), "$2a$10$hash-de-outro");
        when(clienteRepository.findById(FELLIPE)).thenReturn(Optional.of(fellipe));
        when(usuarioRepository.findByLogin("12345678909")).thenReturn(Optional.of(deOutro));

        assertFalse(usuarioService.garantirAcessoDaEquipe(FELLIPE, SENHA_EQUIPE));

        assertEquals("$2a$10$hash-de-outro", deOutro.getSenhaHash());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void semOClienteDoSeedNaoCriaNada() {
        when(clienteRepository.findById(FELLIPE)).thenReturn(Optional.empty());

        assertFalse(usuarioService.garantirAcessoDaEquipe(FELLIPE, SENHA_EQUIPE));

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void recusaEquipeSenhaCurta() {
        assertThrows(IllegalStateException.class, () -> usuarioService.garantirAcessoDaEquipe(FELLIPE, "curta-11b"));
        verifyNoInteractions(clienteRepository, usuarioRepository);
    }

    private static Cliente cliente(UUID id, String cpf) {
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", id);
        cliente.setCpf(cpf);
        return cliente;
    }

    private static Usuario usuarioDo(Cliente cliente, String hash) {
        Usuario usuario = new Usuario();
        usuario.setLogin(cliente.getCpf());
        usuario.setSenhaHash(hash);
        usuario.setPerfil(PerfilEnum.CLIENTE);
        usuario.setCliente(cliente);
        return usuario;
    }
}
