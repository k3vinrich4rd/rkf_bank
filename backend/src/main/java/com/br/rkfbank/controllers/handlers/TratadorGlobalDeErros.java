package com.br.rkfbank.controllers.handlers;

import com.br.rkfbank.config.LogSeguro;

import com.br.rkfbank.dto.comum.ErroCampoResponse;
import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.AcessoNegadoException;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.MuitasTentativasException;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.exceptions.ServicoExternoIndisponivelException;
import com.br.rkfbank.exceptions.UsuarioNaoEncontradoException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Erros de entrada e de concorrencia so aparecem aqui, entao saem em WARN. Os de negocio e de servico externo
// ja foram registrados com contexto (ids, valores) no service/client que os lancou, entao aqui ficam em DEBUG.
@RestControllerAdvice
public class TratadorGlobalDeErros {

    private static final Logger log = LogManager.getLogger(TratadorGlobalDeErros.class);

    // 400 do Bean Validation: @Valid no controller falhou. Junta TODOS os campos com erro numa resposta so,
    // para o front marcar todos os inputs de uma vez (e nao um por tentativa).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse campoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Extrai erros de validacao campo a campo para retorno estruturado ao frontend.
        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();
        // So os nomes dos campos: os valores rejeitados podem ser dados pessoais.
        log.warn("Validacao falhou em {}: campos {}", LogSeguro.texto(request.getRequestURI()),
                LogSeguro.texto(campos.stream().map(ErroCampoResponse::campo).toList()));
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(), campos);
    }

    // 400 quando o Jackson nem consegue ler o corpo: JSON quebrado, texto onde era numero, enum que nao existe.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // So o TIPO do erro: a mensagem do Jackson pode trazer um trecho do valor enviado (dado pessoal, LGPD).
        log.warn("JSON invalido em {}: {}", LogSeguro.texto(request.getRequestURI()), ex.getMostSpecificCause().getClass().getSimpleName());
        return montar(HttpStatus.BAD_REQUEST, "JSON invalido ou tipo de campo incompativel", request.getRequestURI(), List.of());
    }

    // 400 quando um parametro da URL nao converte para o tipo esperado (ex.: "abc" onde era UUID).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // Nome do parametro invalido da URL para orientar quem consome a API.
        String campo = ex.getName();
        log.warn("Parametro de URL invalido em {}: {}", LogSeguro.texto(request.getRequestURI()), LogSeguro.texto(campo));
        return montar(HttpStatus.BAD_REQUEST, "Parametro de URL invalido", request.getRequestURI(), List.of(new ErroCampoResponse(campo, "Tipo invalido para o parametro " + campo)));
    }

    // 400 das validacoes feitas no codigo (page/size, Idempotency-Key, senha em bytes), no mesmo formato do @Valid.
    @ExceptionHandler(RequisicaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        log.warn("Requisicao invalida em {}: {} {}", LogSeguro.texto(request.getRequestURI()), LogSeguro.texto(ex.getCampo()), LogSeguro.texto(ex.getMessage()));
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(),
                List.of(new ErroCampoResponse(ex.getCampo(), ex.getMessage())));
    }

    // 401 do login (CPF ou senha errados). O 401 de token ausente/invalido nasce no filtro e sai pelo
    // TratadorErrosSeguranca, porque o filtro roda antes do controller e este advice nao o enxerga.
    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse credenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // 429 do login: o mesmo CPF errou demais. Retry-After diz em quantos segundos tentar de novo.
    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResponse> muitasTentativas(MuitasTentativasException ex, HttpServletRequest request) {
        ErroResponse corpo = montarELogar(Level.DEBUG, HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaLiberar()))
                .body(corpo);
    }

    // 403 da regra de dono: o usuario esta logado, mas o recurso e de outro cliente.
    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponse acessoNegado(AcessoNegadoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // 404: o recurso pedido nao existe. Os tres "nao encontrado" ficam separados para cada um ter a sua mensagem.
    @ExceptionHandler(ClienteNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse clienteNaoEncontrado(ClienteNaoEncontradoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 404 de conta inexistente.
    @ExceptionHandler(ContaNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse contaNaoEncontrada(ContaNaoEncontradaException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 404 de usuario inexistente (gestao de usuarios do ADMIN).
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse usuarioNaoEncontrado(UsuarioNaoEncontradoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 409: o pedido colide com algo que ja existe (CPF, e-mail ou numero de conta repetidos).
    @ExceptionHandler(ConflitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(ConflitoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 409 do lock otimista (@Version): duas operacoes mexeram na mesma conta ao mesmo tempo e esta perdeu.
    // Nada foi gravado (rollback); repetir a operacao resolve. Mensagem propria porque a causa e diferente do 409
    // de duplicidade, e a do Hibernate (com SQL e nomes de classe) nao pode ir para fora.
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflitoConcorrencia(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrencia em {}: {}", LogSeguro.texto(request.getRequestURI()), ex.getClass().getSimpleName());
        return montar(HttpStatus.CONFLICT, "A conta foi alterada por outra operacao; tente novamente",
                request.getRequestURI(), List.of());
    }

    // 422: regra de negocio recusou (saldo insuficiente, conta inativa, mesma conta...). A mensagem e escrita
    // para o cliente ler, entao vai na resposta como esta.
    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse naoProcessavel(RegraNegocioException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // 503: o ViaCEP falhou. A causa tecnica ja foi logada no ViaCepClient; aqui sai so a mensagem generica.
    @ExceptionHandler(ServicoExternoIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse servicoExterno(ServicoExternoIndisponivelException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    private ErroResponse montarELogar(Level nivel, HttpStatus status, String mensagem, HttpServletRequest request) {
        log.log(nivel, "{} {} em {}: {}", status.value(), status.getReasonPhrase(), LogSeguro.texto(request.getRequestURI()),
                LogSeguro.texto(mensagem));
        return montar(status, mensagem, request.getRequestURI(), List.of());
    }

    private ErroResponse montar(HttpStatus status, String mensagem, String path, List<ErroCampoResponse> campos) {
        // Fabrica central do payload de erro padronizado.
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}
