package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.entities.RegistroIdempotencia;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.repositories.IdempotenciaRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotenciaServiceTest {

    private static final UsuarioLogado USUARIO = new UsuarioLogado(UUID.randomUUID(), UUID.randomUUID(), PerfilEnum.CLIENTE);
    private static final String CHAVE = "3f2c6a1e-8d4b-4c1a-9e7f-0a1b2c3d4e5f";

    @Mock
    private IdempotenciaRepository repository;

    @InjectMocks
    private IdempotenciaService service;

    private final AtomicInteger movimentacoes = new AtomicInteger();

    @Test
    void primeiraChamadaMovimentaEGravaAChave() {
        when(repository.findByUsuarioIdAndChave(USUARIO.usuarioId(), CHAVE)).thenReturn(Optional.empty());

        boolean repetida = service.executarUmaVez(USUARIO, CHAVE, TipoLancamentoEnum.SAQUE, "conta|null|20|", movimentacoes::incrementAndGet);

        assertFalse(repetida);
        assertEquals(1, movimentacoes.get());
        ArgumentCaptor<RegistroIdempotencia> gravado = ArgumentCaptor.forClass(RegistroIdempotencia.class);
        verify(repository).saveAndFlush(gravado.capture());
        assertEquals(USUARIO.usuarioId(), gravado.getValue().getUsuarioId());
        assertEquals(CHAVE, gravado.getValue().getChave());
        assertEquals(TipoLancamentoEnum.SAQUE, gravado.getValue().getOperacao());
        assertEquals(IdempotenciaService.sha256("SAQUE|conta|null|20|"), gravado.getValue().getHashRequisicao());
    }

    @Test
    void repeticaoDaMesmaOperacaoNaoMovimentaDeNovo() {
        when(repository.findByUsuarioIdAndChave(USUARIO.usuarioId(), CHAVE))
                .thenReturn(Optional.of(registro(TipoLancamentoEnum.SAQUE, "conta|null|20|")));

        boolean repetida = service.executarUmaVez(USUARIO, CHAVE, TipoLancamentoEnum.SAQUE, "conta|null|20|", movimentacoes::incrementAndGet);

        assertTrue(repetida);
        assertEquals(0, movimentacoes.get());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void mesmaChaveComOutroValorE422() {
        when(repository.findByUsuarioIdAndChave(USUARIO.usuarioId(), CHAVE))
                .thenReturn(Optional.of(registro(TipoLancamentoEnum.SAQUE, "conta|null|20|")));

        assertThrows(NaoProcessavelException.class, () -> service.executarUmaVez(USUARIO, CHAVE, TipoLancamentoEnum.SAQUE,
                "conta|null|2000|", movimentacoes::incrementAndGet));
        assertEquals(0, movimentacoes.get());
    }

    @Test
    void mesmaChaveEmOutraOperacaoE422() {
        when(repository.findByUsuarioIdAndChave(USUARIO.usuarioId(), CHAVE))
                .thenReturn(Optional.of(registro(TipoLancamentoEnum.DEPOSITO, "conta|null|20|")));

        assertThrows(NaoProcessavelException.class, () -> service.executarUmaVez(USUARIO, CHAVE, TipoLancamentoEnum.SAQUE,
                "conta|null|20|", movimentacoes::incrementAndGet));
    }

    @Test
    void semChaveE400AntesDeMovimentar() {
        RequisicaoInvalidaException erro = assertThrows(RequisicaoInvalidaException.class,
                () -> service.executarUmaVez(USUARIO, null, TipoLancamentoEnum.DEPOSITO, "x", movimentacoes::incrementAndGet));

        assertEquals("Idempotency-Key", erro.getCampo());
        assertEquals(0, movimentacoes.get());
    }

    @Test
    void chaveCurtaOuComEspacoE400() {
        assertThrows(RequisicaoInvalidaException.class,
                () -> service.executarUmaVez(USUARIO, "abc", TipoLancamentoEnum.DEPOSITO, "x", movimentacoes::incrementAndGet));
        assertThrows(RequisicaoInvalidaException.class,
                () -> service.executarUmaVez(USUARIO, "chave com espaco", TipoLancamentoEnum.DEPOSITO, "x", movimentacoes::incrementAndGet));
    }

    @Test
    void chamadaSimultaneaComAMesmaChaveE409() {
        when(repository.findByUsuarioIdAndChave(USUARIO.usuarioId(), CHAVE)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_idempotencia_usuario_chave"));

        // A excecao desfaz a transacao inteira: a movimentacao desta chamada nao fica.
        assertThrows(NegocioException.class, () -> service.executarUmaVez(USUARIO, CHAVE, TipoLancamentoEnum.DEPOSITO,
                "x", movimentacoes::incrementAndGet));
    }

    private static RegistroIdempotencia registro(TipoLancamentoEnum operacao, String assinatura) {
        RegistroIdempotencia registro = new RegistroIdempotencia();
        registro.setUsuarioId(USUARIO.usuarioId());
        registro.setChave(CHAVE);
        registro.setOperacao(operacao);
        registro.setHashRequisicao(IdempotenciaService.sha256(operacao.name() + "|" + assinatura));
        return registro;
    }
}
