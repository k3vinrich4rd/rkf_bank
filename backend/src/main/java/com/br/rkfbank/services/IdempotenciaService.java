package com.br.rkfbank.services;

import com.br.rkfbank.entities.RegistroIdempotencia;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.repositories.IdempotenciaRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Garante que repetir uma movimentacao (rede caiu, clique duplo, retry) nao mexe no dinheiro duas vezes.
// Envolve a chamada do ContaService na MESMA transacao: ou movimenta e grava a chave, ou nao faz nenhum dos dois.
@Service
public class IdempotenciaService {

    public static final String HEADER = "Idempotency-Key";
    // Avisa quem chamou que a resposta e de uma repeticao (nada foi movimentado agora).
    public static final String HEADER_REPETIDA = "Idempotency-Replayed";

    private static final Logger log = LogManager.getLogger(IdempotenciaService.class);

    // UUID e o recomendado; aceita qualquer texto curto sem espaco para nao amarrar o cliente a um formato.
    private static final Pattern FORMATO_CHAVE = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private final IdempotenciaRepository repository;

    public IdempotenciaService(IdempotenciaRepository repository) {
        this.repository = repository;
    }

    // Executa a movimentacao uma unica vez por chave. Tres casos:
    //  1. chave nova: roda a movimentacao e grava a chave (mesma transacao) -> devolve false;
    //  2. chave ja usada para a MESMA operacao (mesma assinatura): nao movimenta de novo -> devolve true, e o
    //     controller responde 204 com Idempotency-Replayed: true, como se tivesse acabado de fazer;
    //  3. chave ja usada para OUTRA operacao (assinatura diferente): 422, porque o cliente reaproveitou a chave errado.
    // "assinatura" = o que identifica a operacao (contas, valor, descricao). So o SHA-256 dela vai para o banco.
    @Transactional
    public boolean executarUmaVez(UsuarioLogado usuario, String chave, TipoLancamentoEnum operacao, String assinatura,
                                  Runnable movimentacao) {
        validarFormato(chave);
        String hash = sha256(operacao.name() + "|" + assinatura);

        Optional<RegistroIdempotencia> existente = repository.findByUsuarioIdAndChave(usuario.usuarioId(), chave);
        if (existente.isPresent()) {
            RegistroIdempotencia registro = existente.get();
            if (registro.getOperacao() != operacao || !registro.getHashRequisicao().equals(hash)) {
                log.warn("Idempotency-Key reutilizada com outra requisicao pelo usuario {} ({})", usuario.usuarioId(), operacao);
                throw new RegraNegocioException("Idempotency-Key ja usada em outra requisicao; gere uma chave nova para esta operacao");
            }
            log.info("Repeticao de {} ignorada pela Idempotency-Key (usuario {})", operacao, usuario.usuarioId());
            return true;
        }

        // Primeira vez com esta chave: movimenta. Se a movimentacao lancar excecao (saldo insuficiente...), a chave
        // nao e gravada, e o cliente pode tentar de novo com a mesma chave depois de corrigir.
        movimentacao.run();

        RegistroIdempotencia novo = new RegistroIdempotencia();
        novo.setUsuarioId(usuario.usuarioId());
        novo.setChave(chave);
        novo.setOperacao(operacao);
        novo.setHashRequisicao(hash);
        try {
            repository.saveAndFlush(novo);
        } catch (DataIntegrityViolationException ex) {
            // Outra chamada com a mesma chave gravou primeiro (corrida). Lancar aqui desfaz a movimentacao desta.
            log.warn("Idempotency-Key em uso por outra chamada simultanea do usuario {} ({})", usuario.usuarioId(), operacao);
            throw new ConflitoException("Esta operacao ja esta sendo processada; repita com a mesma Idempotency-Key para ver o resultado");
        }
        return false;
    }

    private static void validarFormato(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new RequisicaoInvalidaException(HEADER,
                    "header Idempotency-Key e obrigatorio nas movimentacoes (gere um UUID novo por operacao)");
        }
        if (!FORMATO_CHAVE.matcher(chave).matches()) {
            throw new RequisicaoInvalidaException(HEADER,
                    "Idempotency-Key deve ter de 8 a 100 caracteres: letras, numeros, - ou _");
        }
    }

    // Resumo de tamanho fixo (64 caracteres hex) da operacao: compara "e a mesma requisicao?" sem guardar os
    // dados dela, e cabe numa coluna pequena qualquer que seja o tamanho da descricao.
    static String sha256(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 e obrigatorio em toda JVM; chegar aqui e defeito do ambiente.
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }
}
