package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acesso a tb_usuario (credenciais). O login e sempre o CPF so com digitos.
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    // Busca usada no login (login = CPF so com digitos).
    Optional<Usuario> findByLogin(String login);
    // Credencial do cliente logado (encerramento e anonimizacao LGPD).
    Optional<Usuario> findByClienteId(UUID clienteId);
    // Usado na criacao do admin para nao duplicar.
    boolean existsByLogin(String login);
    // Tela de gestao: admins primeiro, depois por login. A lista mostra o nome do cliente: o @EntityGraph traz o
    // cliente na mesma consulta (o Usuario.cliente e LAZY), em vez de uma consulta por usuario (N+1).
    @EntityGraph(attributePaths = "cliente")
    List<Usuario> findAllByOrderByPerfilAscLoginAsc();
}
