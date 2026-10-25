package com.br.rkfbank.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.br.rkfbank.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InicializadorAcessoEquipeTest {

    @Mock
    private UsuarioService usuarioService;

    @Test
    void daLoginAsDuasContasDaEquipeQuandoAEquipeSenhaEstaNoEnv() {
        new InicializadorAcessoEquipe(new EquipePropriedades("senha-da-equipe-de-teste"), usuarioService).run(null);

        for (var clienteId : InicializadorAcessoEquipe.CLIENTES_DA_EQUIPE) {
            verify(usuarioService).garantirAcessoDaEquipe(clienteId, "senha-da-equipe-de-teste");
        }
    }

    @Test
    void semEquipeSenhaNinguemEntraNasContasDaEquipe() {
        new InicializadorAcessoEquipe(new EquipePropriedades(""), usuarioService).run(null);
        new InicializadorAcessoEquipe(new EquipePropriedades(null), usuarioService).run(null);

        verifyNoInteractions(usuarioService);
    }
}
