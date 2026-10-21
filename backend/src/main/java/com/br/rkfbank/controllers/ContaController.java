package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.ContaService;
import com.br.rkfbank.services.IdempotenciaService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Metodo, rota, status e duracao de cada chamada ja saem no FiltroLogRequisicao; aqui so o detalhe em DEBUG.
// Todas as rotas exigem token (ConfiguracaoSeguranca); o Jwt chega ja validado e vira UsuarioLogado para o service.
@RestController
@RequestMapping("/contas")
public class ContaController {

    private static final Logger log = LogManager.getLogger(ContaController.class);

    private final ContaService contaService;
    private final IdempotenciaService idempotenciaService;

    public ContaController(ContaService contaService, IdempotenciaService idempotenciaService) {
        this.contaService = contaService;
        this.idempotenciaService = idempotenciaService;
    }

    @PostMapping("/clientes/{clienteId}")
    public ResponseEntity<ContaResponseDto> abrir(@PathVariable UUID clienteId, @Valid @RequestBody AbrirContaRequestDto request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        // Criação de conta vinculada ao cliente com retorno 201.
        log.debug("Recebida abertura de conta {} para cliente {}", request.tipoContaEnum(), clienteId);
        return ResponseEntity.status(201).body(contaService.abrir(clienteId, request, UsuarioLogado.de(jwt)));
    }

    // Movimentacoes exigem o header Idempotency-Key: repetir a chamada com a mesma chave nao movimenta de novo.
    @PostMapping("/{contaId}/deposito")
    public ResponseEntity<Void> depositar(@PathVariable UUID contaId, @Valid @RequestBody DepositarRequestDto request,
                                          @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                          @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebido deposito de {} na conta {}", request.valor(), contaId);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.DEPOSITO,
                assinatura(contaId, null, request.valor(), request.descricao()),
                () -> contaService.depositar(contaId, request, usuario));
        return semConteudo(repetida);
    }

    @PostMapping("/{contaId}/saque")
    public ResponseEntity<Void> sacar(@PathVariable UUID contaId, @Valid @RequestBody SaqueRequestDto request,
                                      @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                      @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebido saque de {} na conta {}", request.valor(), contaId);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.SAQUE,
                assinatura(contaId, null, request.valor(), request.descricao()),
                () -> contaService.sacar(contaId, request, usuario));
        return semConteudo(repetida);
    }

    @PostMapping("/transferencia")
    public ResponseEntity<Void> transferir(@Valid @RequestBody TransferirRequestDto request,
                                           @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                           @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebida transferencia de {} da conta {} para {}",
                request.valor(), request.contaOrigemId(), request.contaDestinoId());
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.TRANSFERENCIA,
                assinatura(request.contaOrigemId(), request.contaDestinoId(), request.valor(), request.descricao()),
                () -> contaService.transferir(request, usuario));
        return semConteudo(repetida);
    }

    // 204 nos dois casos; a repeticao avisa pelo header, para o cliente saber que nada foi movimentado agora.
    private static ResponseEntity<Void> semConteudo(boolean repetida) {
        ResponseEntity.HeadersBuilder<?> resposta = ResponseEntity.noContent();
        if (repetida) {
            resposta.header(IdempotenciaService.HEADER_REPETIDA, "true");
        }
        return resposta.build();
    }

    // "10.0" e "10.00" sao o mesmo valor: stripTrailingZeros evita que a repeticao pareca outra operacao.
    private static String assinatura(UUID conta, UUID destino, BigDecimal valor, String descricao) {
        return conta + "|" + destino + "|" + valor.stripTrailingZeros().toPlainString() + "|" + (descricao == null ? "" : descricao);
    }

    @GetMapping("/{contaId}/lancamentos")
    public ResponseEntity<List<LancamentoResponseDto>> listar(@PathVariable UUID contaId, @AuthenticationPrincipal Jwt jwt) {
        // Retorna extrato da conta em ordem decrescente por data.
        log.debug("Consulta de extrato da conta {}", contaId);
        return ResponseEntity.ok(contaService.listarLancamentos(contaId, UsuarioLogado.de(jwt)));
    }

    @GetMapping
    public ResponseEntity<?> listarContas(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.debug("Listagem de contas (paginado={}, page={}, size={})", paginado, page, size);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        // Quando paginado=true, retorna Page com metadados de paginação. ADMIN ve todas; CLIENTE so as dele.
        if (paginado) {
            Page<ContaResponseDto> resposta = contaService.listarPaginado(Paginacao.criar(page, size), usuario);
            return ResponseEntity.ok(resposta);
        }

        // Caso contrário, retorna lista completa.
        List<ContaResponseDto> resposta = contaService.listarTodas(usuario);
        return ResponseEntity.ok(resposta);
    }
}
