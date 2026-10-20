package com.br.rkfbank.repositories;

import com.br.rkfbank.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    // Busca usada no login (login = CPF so com digitos).
    Optional<Usuario> findByLogin(String login);
    // Usado na criacao do admin para nao duplicar.
    boolean existsByLogin(String login);
    // Tela de gestao: admins primeiro, depois por login.
    List<Usuario> findAllByOrderByPerfilAscLoginAsc();
}
