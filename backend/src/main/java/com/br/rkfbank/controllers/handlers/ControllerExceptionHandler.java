package com.br.rkfbank.controllers.handlers;

import com.br.rkfbank.dto.comum.ErroCampoResponse;
import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.AcessoNegadoException;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.exceptions.ServicoExternoException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Erros de entrada e de concorrencia so aparecem aqui, entao saem em WARN. Os de negocio e de servico externo
// ja foram registrados com contexto (ids, valores) no service/client que os lancou, entao aqui ficam em DEBUG.
@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger log = LogManager.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse campoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Extrai erros de validacao campo a campo para retorno estruturado ao frontend.
        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();
        // So os nomes dos campos: os valores rejeitados podem ser dados pessoais.
        log.warn("Validacao falhou em {}: campos {}", request.getRequestURI(),
                campos.stream().map(ErroCampoResponse::campo).toList());
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(), campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("JSON invalido em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.BAD_REQUEST, "JSON invalido ou tipo de campo incompativel", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // Nome do parametro invalido da URL para orientar quem consome a API.
        String campo = ex.getName();
        log.warn("Parametro de URL invalido em {}: {}", request.getRequestURI(), campo);
        return montar(HttpStatus.BAD_REQUEST, "Parametro de URL invalido", request.getRequestURI(), List.of(new ErroCampoResponse(campo, "Tipo invalido para o parametro " + campo)));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        log.warn("Requisicao invalida em {}: {} {}", request.getRequestURI(), ex.getCampo(), ex.getMessage());
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(),
                List.of(new ErroCampoResponse(ex.getCampo(), ex.getMessage())));
    }

    // 401 do login (CPF ou senha errados). O 401 de token ausente/invalido nasce no filtro e sai pelo
    // RespostaErroSeguranca, porque o filtro roda antes do controller e este advice nao o enxerga.
    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse credenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // 403 da regra de dono: o usuario esta logado, mas o recurso e de outro cliente.
    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponse acessoNegado(AcessoNegadoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse clienteNaoEncontrado(ClienteNaoEncontradoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse contaNaoEncontrada(ContaNaoEncontradaException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(NegocioException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflitoConcorrencia(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrencia em {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return montar(HttpStatus.CONFLICT, "A conta foi alterada por outra operacao; tente novamente",
                request.getRequestURI(), List.of());
    }

    @ExceptionHandler(NaoProcessavelException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse naoProcessavel(NaoProcessavelException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicoExternoException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse servicoExterno(ServicoExternoException ex, HttpServletRequest request) {
        return montarELogar(Level.DEBUG, HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    private ErroResponse montarELogar(Level nivel, HttpStatus status, String mensagem, HttpServletRequest request) {
        log.log(nivel, "{} {} em {}: {}", status.value(), status.getReasonPhrase(), request.getRequestURI(), mensagem);
        return montar(status, mensagem, request.getRequestURI(), List.of());
    }

    private ErroResponse montar(HttpStatus status, String mensagem, String path, List<ErroCampoResponse> campos) {
        // Fabrica central do payload de erro padronizado.
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}
