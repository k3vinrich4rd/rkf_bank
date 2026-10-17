package com.br.rkfbank.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UsuarioRepositoryTest {

    // Hash BCrypt de exemplo: 60 caracteres, o tamanho da coluna senha_hash.
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuW0wC2rXJbWm3l3a4b5c6d7e8f9g0h1i";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void deveSalvarUsuarioDoClienteEBuscarPeloLogin() {
        Cliente cliente = entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));
        usuarioRepository.saveAndFlush(novoUsuario("52998224725", PerfilEnum.CLIENTE, cliente));
        entityManager.clear();

        Usuario encontrado = usuarioRepository.findByLogin("52998224725").orElseThrow();

        assertEquals(HASH, encontrado.getSenhaHash());
        assertEquals(PerfilEnum.CLIENTE, encontrado.getPerfil());
        assertEquals(cliente.getId(), encontrado.getCliente().getId());
    }

    @Test
    void deveSalvarAdminSemCliente() {
        usuarioRepository.saveAndFlush(novoUsuario("11144477735", PerfilEnum.ADMIN, null));

        Usuario admin = usuarioRepository.findByLogin("11144477735").orElseThrow();

        assertEquals(PerfilEnum.ADMIN, admin.getPerfil());
        assertNull(admin.getCliente());
    }

    @Test
    void deveInformarSeLoginExiste() {
        usuarioRepository.saveAndFlush(novoUsuario("11144477735", PerfilEnum.ADMIN, null));

        assertTrue(usuarioRepository.existsByLogin("11144477735"));
        assertFalse(usuarioRepository.existsByLogin("52998224725"));
    }

    @Test
    void deveViolarUnicidadeAoRepetirLogin() {
        usuarioRepository.saveAndFlush(novoUsuario("11144477735", PerfilEnum.ADMIN, null));

        Usuario duplicado = novoUsuario("11144477735", PerfilEnum.ADMIN, null);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(duplicado));
    }

    @Test
    void deveViolarUnicidadeAoDarDoisUsuariosParaOMesmoCliente() {
        Cliente cliente = entityManager.persistAndFlush(novoCliente("52998224725", "joao@rkfbank.com"));
        usuarioRepository.saveAndFlush(novoUsuario("52998224725", PerfilEnum.CLIENTE, cliente));

        Usuario segundo = novoUsuario("11144477735", PerfilEnum.CLIENTE, cliente);

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(segundo));
    }

    private Usuario novoUsuario(String login, PerfilEnum perfil, Cliente cliente) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(HASH);
        usuario.setPerfil(perfil);
        usuario.setCliente(cliente);
        return usuario;
    }

    private Cliente novoCliente(String cpf, String email) {
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");

        Cliente cliente = new Cliente();
        cliente.setNomeCompleto("Joao Silva");
        cliente.setCpf(cpf);
        cliente.setEmail(email);
        cliente.setTelefone("11999999999");
        cliente.setEndereco(endereco);
        return cliente;
    }
}
