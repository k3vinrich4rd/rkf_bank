package com.br.rkfbank.controllers.handlers;

import com.br.rkfbank.dto.comum.ErroCampoResponse;
import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.exceptions.ServicoExternoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse campoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Extrai erros de validacao campo a campo para retorno estruturado ao frontend.
        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(), campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "JSON invalido ou tipo de campo incompativel", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // Nome do parametro invalido da URL para orientar quem consome a API.
        String campo = ex.getName();
        return montar(HttpStatus.BAD_REQUEST, "Parametro de URL invalido", request.getRequestURI(), List.of(new ErroCampoResponse(campo, campo + " deve ser um UUID valido")));
    }

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse clienteNaoEncontrado(ClienteNaoEncontradoException ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse contaNaoEncontrada(ContaNaoEncontradaException ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(NegocioException ex, HttpServletRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(NaoProcessavelException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse naoProcessavel(NaoProcessavelException ex, HttpServletRequest request) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ServicoExternoException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse servicoExterno(ServicoExternoException ex, HttpServletRequest request) {
        return montar(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(), List.of());
    }

    private ErroResponse montar(HttpStatus status, String mensagem, String path, List<ErroCampoResponse> campos) {
        // Fabrica central do payload de erro padronizado.
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}