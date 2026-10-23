package com.br.rkfbank.config;

import com.br.rkfbank.services.UsuarioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Roda uma vez na subida (depois do Flyway) e cria o primeiro ADMIN a partir do .env.
// Senha de admin em migracao ficaria no Git para sempre; aqui ela so existe no ambiente.
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(InicializadorAdmin.class);

    private final AdminPropriedades propriedades;
    private final UsuarioService usuarioService;

    public InicializadorAdmin(AdminPropriedades propriedades, UsuarioService usuarioService) {
        this.propriedades = propriedades;
        this.usuarioService = usuarioService;
    }

    // Roda uma vez na subida. "SeNaoExistir": reiniciar a API 100 vezes cria o admin uma vez so (idempotente).
    @Override
    public void run(ApplicationArguments args) {
        if (!propriedades.configurado()) {
            log.info("ADMIN_CPF/ADMIN_SENHA nao definidos; nenhum admin criado");
            return;
        }
        usuarioService.criarAdminSeNaoExistir(propriedades.cpf(), propriedades.senha());
    }
}
