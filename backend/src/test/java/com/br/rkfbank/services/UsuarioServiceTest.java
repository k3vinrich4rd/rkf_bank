package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.repositories.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}
