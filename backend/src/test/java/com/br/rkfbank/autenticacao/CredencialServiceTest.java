package com.br.rkfbank.autenticacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.clientes.Cliente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

// A credencial que o cadastro pede ao modulo autenticacao: login = CPF, so o hash BCrypt no banco, perfil CLIENTE.
@ExtendWith(MockitoExtension.class)
class CredencialServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CredencialService credencialService;

    @Test
    void deveCriarUsuarioClienteComSenhaEmHash() {
        Cliente cliente = new Cliente();
        when(passwordEncoder.encode("senha-forte-1")).thenReturn("$2a$10$hash-gerado-pelo-bcrypt");

        credencialService.criarParaCliente(cliente, "52998224725", "senha-forte-1");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario usuario = captor.getValue();
        assertEquals("52998224725", usuario.getLogin());
        assertEquals("$2a$10$hash-gerado-pelo-bcrypt", usuario.getSenhaHash());
        assertEquals(PerfilEnum.CLIENTE, usuario.getPerfil());
        assertSame(cliente, usuario.getCliente());
    }

    @Test
    void deveDizerQueOLoginJaEstaEmUso() {
        when(usuarioRepository.existsByLogin("11144477735")).thenReturn(true);

        assertTrue(credencialService.loginEmUso("11144477735"));
    }
}
