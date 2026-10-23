package com.br.rkfbank.controllers.handlers;

import com.br.rkfbank.config.LogSeguro;
import com.br.rkfbank.dto.comum.ErroCampoResponse;
import com.br.rkfbank.dto.comum.ErroResponse;
import com.br.rkfbank.exceptions.AcessoNegadoException;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.CredenciaisInvalidasException;
import com.br.rkfbank.exceptions.ErroApiException;
import com.br.rkfbank.exceptions.MuitasTentativasException;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.exceptions.ServicoExternoIndisponivelException;
import com.br.rkfbank.exceptions.SessaoExpiradaException;
import com.br.rkfbank.exceptions.UsuarioNaoEncontradoException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
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

// Traduz excecao em resposta HTTP. Aqui so se decide o STATUS: o codigo e a mensagem vem do CodigoErroEnum que a
// excecao carrega, entao nenhum texto de erro e escrito neste arquivo.
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
        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();
        // So os nomes dos campos: os valores rejeitados podem ser dados pessoais.
        log.warn("Validacao falhou em {}: campos {}", LogSeguro.texto(request.getRequestURI()),
                LogSeguro.texto(campos.stream().map(ErroCampoResponse::campo).toList()));
        return montar(HttpStatus.BAD_REQUEST, CodigoErroEnum.CAMPOS_INVALIDOS, request, campos);
    }

    // 400 quando o Jackson nem consegue ler o corpo: JSON quebrado, texto onde era numero, enum que nao existe.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // So o TIPO do erro: a mensagem do Jackson pode trazer um trecho do valor enviado (dado pessoal, LGPD).
        log.warn("JSON invalido em {}: {}", LogSeguro.texto(request.getRequestURI()), ex.getMostSpecificCause().getClass().getSimpleName());
        return montar(HttpStatus.BAD_REQUEST, CodigoErroEnum.JSON_INVALIDO, request, List.of());
    }

    // 400 quando um parametro da URL nao converte para o tipo esperado (ex.: "abc" onde era UUID).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String campo = ex.getName();
        log.warn("Parametro de URL invalido em {}: {}", LogSeguro.texto(request.getRequestURI()), LogSeguro.texto(campo));
        return montar(HttpStatus.BAD_REQUEST, CodigoErroEnum.PARAMETRO_INVALIDO, request,
                List.of(new ErroCampoResponse(campo, "Tipo inválido para o parâmetro " + campo)));
    }

    // 400 das validacoes feitas no codigo (page/size, Idempotency-Key, senha em bytes), no mesmo formato do @Valid.
    @ExceptionHandler(RequisicaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        log.warn("Requisicao invalida em {}: {} {}", LogSeguro.texto(request.getRequestURI()), LogSeguro.texto(ex.getCampo()), LogSeguro.texto(ex.getMessage()));
        return montar(HttpStatus.BAD_REQUEST, CodigoErroEnum.CAMPOS_INVALIDOS, request,
                List.of(new ErroCampoResponse(ex.getCampo(), ex.getMessage())));
    }

    // 401 do login (CPF ou senha errados). O 401 de token ausente/invalido nasce no filtro e sai pelo
    // TratadorErrosSeguranca, porque o filtro roda antes do controller e este advice nao o enxerga.
    // Tambem o 401 da renovacao de sessao (refresh token vencido, revogado ou reusado).
    @ExceptionHandler({CredenciaisInvalidasException.class, SessaoExpiradaException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse credenciaisInvalidas(ErroApiException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.UNAUTHORIZED, ex, request);
    }

    // 429 do login: o mesmo CPF errou demais. Retry-After diz em quantos segundos tentar de novo.
    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResponse> muitasTentativas(MuitasTentativasException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaLiberar()))
                .body(montarELogar(HttpStatus.TOO_MANY_REQUESTS, ex, request));
    }

    // 403 da regra de dono: o usuario esta logado, mas a conta e de outro cliente.
    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponse acessoNegado(AcessoNegadoException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.FORBIDDEN, ex, request);
    }

    // 404: o recurso pedido nao existe (cliente, conta ou usuario; o codigo diz qual).
    @ExceptionHandler({ClienteNaoEncontradoException.class, ContaNaoEncontradaException.class, UsuarioNaoEncontradoException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse naoEncontrado(ErroApiException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.NOT_FOUND, ex, request);
    }

    // 409: o pedido colide com algo que ja existe (CPF, e-mail, numero de conta) ou com uma chamada simultanea.
    @ExceptionHandler(ConflitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(ConflitoException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.CONFLICT, ex, request);
    }

    // 409 do lock otimista (@Version): duas operacoes mexeram na mesma conta ao mesmo tempo e esta perdeu.
    // Nada foi gravado (rollback); repetir a operacao resolve. A mensagem do Hibernate (com SQL e nomes de classe)
    // nunca vai para fora.
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflitoConcorrencia(RuntimeException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrencia em {}: {}", LogSeguro.texto(request.getRequestURI()), ex.getClass().getSimpleName());
        return montar(HttpStatus.CONFLICT, CodigoErroEnum.CONCORRENCIA, request, List.of());
    }

    // 422: regra de negocio recusou (saldo insuficiente, conta inativa, mesma conta...). O codigo diz qual.
    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
    }

    // 503: o ViaCEP falhou. A causa tecnica ja foi logada no ViaCepClient; aqui sai so a mensagem do CodigoErroEnum.
    @ExceptionHandler(ServicoExternoIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse servicoExterno(ServicoExternoIndisponivelException ex, HttpServletRequest request) {
        return montarELogar(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
    }

    private ErroResponse montarELogar(HttpStatus status, ErroApiException ex, HttpServletRequest request) {
        log.debug("{} {} em {}: {}", status.value(), ex.getCodigo(), LogSeguro.texto(request.getRequestURI()), ex.getMessage());
        return montar(status, ex.getCodigo(), request, List.of());
    }

    // Fabrica central do payload de erro padronizado.
    private ErroResponse montar(HttpStatus status, CodigoErroEnum codigo, HttpServletRequest request, List<ErroCampoResponse> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), codigo, codigo.getMensagem(),
                request.getRequestURI(), campos);
    }
}
