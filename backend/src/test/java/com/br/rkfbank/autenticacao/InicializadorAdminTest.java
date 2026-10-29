package com.br.rkfbank.autenticacao;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InicializadorAdminTest {

    @Mock
    private UsuarioService usuarioService;

    @Test
    void criaAdminQuandoCpfESenhaEstaoNoEnv() {
        new InicializadorAdmin(new AdminPropriedades("11144477735", "senha-do-admin"), usuarioService).run(null);

        verify(usuarioService).criarAdminSeNaoExistir("11144477735", "senha-do-admin");
    }

    @Test
    void naoFazNadaSemAsVariaveis() {
        new InicializadorAdmin(new AdminPropriedades("", ""), usuarioService).run(null);
        new InicializadorAdmin(new AdminPropriedades(null, "senha-do-admin"), usuarioService).run(null);

        verifyNoInteractions(usuarioService);
    }
}
