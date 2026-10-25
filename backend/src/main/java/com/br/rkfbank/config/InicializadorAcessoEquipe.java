package com.br.rkfbank.config;

import com.br.rkfbank.services.UsuarioService;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Roda na subida, depois do Flyway: da login as contas da equipe criadas pelo db/seed/equipe, com a senha do .env.
// Mesmo motivo do InicializadorAdmin: senha em migracao ficaria no Git (repositorio publico) para sempre.
// Trocou EQUIPE_SENHA? Na proxima subida a senha nova passa a valer (a antiga para de funcionar).
@Component
public class InicializadorAcessoEquipe implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(InicializadorAcessoEquipe.class);

    // Ids fixos do afterMigrate__dados_equipe.sql: "Fellipe Santana (Teste Origem)" e "Renan Barros (Teste Destino)".
    static final List<UUID> CLIENTES_DA_EQUIPE = List.of(
            UUID.fromString("5eed0000-0000-4000-8000-000000000012"),
            UUID.fromString("5eed0000-0000-4000-8000-000000000022"));

    private final EquipePropriedades propriedades;
    private final UsuarioService usuarioService;

    public InicializadorAcessoEquipe(EquipePropriedades propriedades, UsuarioService usuarioService) {
        this.propriedades = propriedades;
        this.usuarioService = usuarioService;
    }

    // ApplicationRunner.run: o Spring chama uma vez, depois que a aplicacao subiu inteira (Flyway ja rodou, entao
    // as tabelas e o seed existem). E o lugar certo para preparar dados que dependem de configuracao do ambiente.
    @Override
    public void run(ApplicationArguments args) {
        if (!propriedades.configurado()) {
            log.info("EQUIPE_SENHA nao definida; contas da equipe sem login");
            return;
        }
        int liberadas = 0;
        for (UUID clienteId : CLIENTES_DA_EQUIPE) {
            if (usuarioService.garantirAcessoDaEquipe(clienteId, propriedades.senha())) {
                liberadas++;
            }
        }
        log.info("Acesso da equipe conferido: {} de {} contas com login", liberadas, CLIENTES_DA_EQUIPE.size());
    }
}
