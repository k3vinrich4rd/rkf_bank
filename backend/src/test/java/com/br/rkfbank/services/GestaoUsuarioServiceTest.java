package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.response.usuario.UsuarioAdminResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.UsuarioNaoEncontradoException;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GestaoUsuarioServiceTest {

    private static final UsuarioLogado ADMIN = new UsuarioLogado(UUID.randomUUID(), null, PerfilEnum.ADMIN);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GestaoUsuarioService service;

    @Test
    void promoveClienteAAdmin() {
        Usuario felipe = usuario(PerfilEnum.CLIENTE, "12345678909", "Felipe Santana");
        when(usuarioRepository.findById(felipe.getId())).thenReturn(Optional.of(felipe));

        UsuarioAdminResponseDto resposta = service.alterarPerfil(felipe.getId(), PerfilEnum.ADMIN, ADMIN);

        assertEquals(PerfilEnum.ADMIN, felipe.getPerfil());
        assertEquals(PerfilEnum.ADMIN, resposta.perfil());
        assertEquals("***.456.789-**", resposta.cpf());
        assertEquals("Felipe Santana", resposta.nome());
    }

    @Test
    void rebaixaAdminQueTemCadastroDeCliente() {
        Usuario felipe = usuario(PerfilEnum.ADMIN, "12345678909", "Felipe Santana");
        when(usuarioRepository.findById(felipe.getId())).thenReturn(Optional.of(felipe));

        service.alterarPerfil(felipe.getId(), PerfilEnum.CLIENTE, ADMIN);

        assertEquals(PerfilEnum.CLIENTE, felipe.getPerfil());
    }

    @Test
    void adminSemCadastroNaoViraCliente() {
        Usuario adminDoEnv = usuario(PerfilEnum.ADMIN, "13579246828", null);
        when(usuarioRepository.findById(adminDoEnv.getId())).thenReturn(Optional.of(adminDoEnv));

        assertThrows(NaoProcessavelException.class, () -> service.alterarPerfil(adminDoEnv.getId(), PerfilEnum.CLIENTE, ADMIN));
        assertEquals(PerfilEnum.ADMIN, adminDoEnv.getPerfil());
    }

    @Test
    void desativaEReativaOAcesso() {
        Usuario renan = usuario(PerfilEnum.CLIENTE, "98765432100", "Renan Barros");
        when(usuarioRepository.findById(renan.getId())).thenReturn(Optional.of(renan));

        assertFalse(service.alterarAcesso(renan.getId(), false, ADMIN).ativo());
        assertFalse(renan.isAtivo());
        assertTrue(service.alterarAcesso(renan.getId(), true, ADMIN).ativo());
        assertTrue(renan.isAtivo());
    }

    @Test
    void ninguemMexeNoProprioPerfilOuAcesso() {
        // Garante que o admin nao se tranca para fora e que sempre sobra um ADMIN ativo.
        assertThrows(NaoProcessavelException.class, () -> service.alterarPerfil(ADMIN.usuarioId(), PerfilEnum.CLIENTE, ADMIN));
        assertThrows(NaoProcessavelException.class, () -> service.alterarAcesso(ADMIN.usuarioId(), false, ADMIN));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void adminRedefineASenhaDeOutroESobeAVersao() {
        Usuario renan = usuario(PerfilEnum.CLIENTE, "98765432100", "Renan Barros");
        when(usuarioRepository.findById(renan.getId())).thenReturn(Optional.of(renan));
        when(passwordEncoder.encode("senha-temporaria-1")).thenReturn("$2a$10$hash-temporario");

        service.redefinirSenha(renan.getId(), "senha-temporaria-1", ADMIN);

        assertEquals("$2a$10$hash-temporario", renan.getSenhaHash());
        assertEquals(1, renan.getVersaoCredencial());
    }

    @Test
    void adminNaoRedefineAPropriaSenhaPorAqui() {
        NaoProcessavelException ex = assertThrows(NaoProcessavelException.class,
                () -> service.redefinirSenha(ADMIN.usuarioId(), "senha-temporaria-1", ADMIN));

        assertEquals("Para trocar a sua propria senha use PATCH /auth/senha", ex.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void usuarioInexistenteE404() {
        UUID qualquer = UUID.randomUUID();
        when(usuarioRepository.findById(qualquer)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontradoException.class, () -> service.alterarAcesso(qualquer, false, ADMIN));
    }

    @Test
    void listaComCpfMascaradoESemCadastroParaOAdminDoEnv() {
        when(usuarioRepository.findAllByOrderByPerfilAscLoginAsc())
                .thenReturn(List.of(usuario(PerfilEnum.ADMIN, "13579246828", null)));

        UsuarioAdminResponseDto admin = service.listar().getFirst();

        assertEquals("***.792.468-**", admin.cpf());
        assertNull(admin.nome());
        assertNull(admin.clienteId());
    }

    @Test
    void cpfForaDoFormatoNaoApareceNemEmParte() {
        assertEquals("***", GestaoUsuarioService.mascararCpf("123"));
        assertEquals("***", GestaoUsuarioService.mascararCpf(null));
    }

    private static Usuario usuario(PerfilEnum perfil, String cpf, String nome) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setLogin(cpf);
        usuario.setPerfil(perfil);
        if (nome != null) {
            Cliente cliente = new Cliente();
            ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
            cliente.setNomeCompleto(nome);
            usuario.setCliente(cliente);
        }
        return usuario;
    }
}
