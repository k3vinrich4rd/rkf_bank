package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.comum.PaginaResponse;
import com.br.rkfbank.dto.request.conta.AberturaContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositoRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferenciaRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.ContaService;
import com.br.rkfbank.services.IdempotenciaService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Metodo, rota, status e duracao de cada chamada ja saem no FiltroLogRequisicao; aqui so o detalhe em DEBUG.
// Todas as rotas exigem token (ConfiguracaoSeguranca); o Jwt chega ja validado e vira UsuarioLogado para o service.
@Tag(name = "Contas e dinheiro", description = "abrir conta, depositar, sacar, transferir e extrato")
@RestController
public class ContaController {

    private static final Logger log = LogManager.getLogger(ContaController.class);

    private final ContaService contaService;
    private final IdempotenciaService idempotenciaService;

    public ContaController(ContaService contaService, IdempotenciaService idempotenciaService) {
        this.contaService = contaService;
        this.idempotenciaService = idempotenciaService;
    }

    // Abre conta no cadastro de quem esta logado: o cliente vem do token (ContaService.abrir), nunca da URL.
    // 201 Created com a conta como ficou salva (agencia 0001 e numero gerados pelo banco).
    @PostMapping("/contas")
    public ResponseEntity<ContaResponseDto> abrir(@Valid @RequestBody AberturaContaRequestDto request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebida abertura de conta {}", request.tipoConta());
        return ResponseEntity.status(201).body(contaService.abrir(request, UsuarioLogado.de(jwt)));
    }

    // Movimentacoes exigem o header Idempotency-Key: repetir a chamada com a mesma chave nao movimenta de novo.
    @PostMapping("/contas/{contaId}/depositos")
    public ResponseEntity<Void> depositar(@PathVariable UUID contaId, @Valid @RequestBody DepositoRequestDto request,
                                          @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                          @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebido deposito de {} na conta {}", request.valor(), contaId);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.DEPOSITO,
                assinaturaDaOperacao(contaId, null, request.valor(), request.descricao()),
                () -> contaService.depositar(contaId, request, usuario));
        return respostaSemCorpo(repetida);
    }

    // Mesmo desenho do deposito: a movimentacao so roda dentro do IdempotenciaService.executarUmaVez.
    @PostMapping("/contas/{contaId}/saques")
    public ResponseEntity<Void> sacar(@PathVariable UUID contaId, @Valid @RequestBody SaqueRequestDto request,
                                      @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                      @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebido saque de {} na conta {}", request.valor(), contaId);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.SAQUE,
                assinaturaDaOperacao(contaId, null, request.valor(), request.descricao()),
                () -> contaService.sacar(contaId, request, usuario));
        return respostaSemCorpo(repetida);
    }

    // A "assinatura" (contas + valor + descricao) e o que torna uma repeticao reconhecivel: mesma chave com a
    // mesma assinatura = repeticao (204 sem mover dinheiro); mesma chave com outra = erro do cliente (422).
    @PostMapping("/transferencias")
    public ResponseEntity<Void> transferir(@Valid @RequestBody TransferenciaRequestDto request,
                                           @RequestHeader(name = IdempotenciaService.HEADER, required = false) String chave,
                                           @AuthenticationPrincipal Jwt jwt) {
        log.debug("Recebida transferencia de {} da conta {} para {}",
                request.valor(), request.contaOrigemId(), request.contaDestinoId());
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        boolean repetida = idempotenciaService.executarUmaVez(usuario, chave, TipoLancamentoEnum.TRANSFERENCIA,
                assinaturaDaOperacao(request.contaOrigemId(), request.contaDestinoId(), request.valor(), request.descricao()),
                () -> contaService.transferir(request, usuario));
        return respostaSemCorpo(repetida);
    }

    // 204 nos dois casos; a repeticao avisa pelo header, para o cliente saber que nada foi movimentado agora.
    private static ResponseEntity<Void> respostaSemCorpo(boolean repetida) {
        ResponseEntity.HeadersBuilder<?> resposta = ResponseEntity.noContent();
        if (repetida) {
            resposta.header(IdempotenciaService.HEADER_REPETIDA, "true");
        }
        return resposta.build();
    }

    // "10.0" e "10.00" sao o mesmo valor: stripTrailingZeros evita que a repeticao pareca outra operacao.
    private static String assinaturaDaOperacao(UUID conta, UUID destino, BigDecimal valor, String descricao) {
        return conta + "|" + destino + "|" + valor.stripTrailingZeros().toPlainString() + "|" + (descricao == null ? "" : descricao);
    }

    // Uma conta so: saldo, tipo e se esta ativa. CLIENTE ve as dele (403 nas outras); ADMIN ve qualquer uma.
    @GetMapping("/contas/{contaId}")
    public ResponseEntity<ContaResponseDto> buscar(@PathVariable UUID contaId, @AuthenticationPrincipal Jwt jwt) {
        log.debug("Consulta da conta {}", contaId);
        return ResponseEntity.ok(contaService.buscarPorId(contaId, UsuarioLogado.de(jwt)));
    }

    // Extrato da conta (GET /contas/{contaId}/lancamentos). Regra de dono no service.
    @GetMapping("/contas/{contaId}/lancamentos")
    public ResponseEntity<List<LancamentoResponseDto>> extrato(@PathVariable UUID contaId, @AuthenticationPrincipal Jwt jwt) {
        // Retorna extrato da conta em ordem decrescente por data.
        log.debug("Consulta de extrato da conta {}", contaId);
        return ResponseEntity.ok(contaService.listarLancamentos(contaId, UsuarioLogado.de(jwt)));
    }

    // Contas do usuario logado (ou todas, para o ADMIN), em lista ou paginadas.
    @GetMapping("/contas")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.debug("Listagem de contas (page={}, size={})", page, size);
        UsuarioLogado usuario = UsuarioLogado.de(jwt);
        // A pagina sai no formato do contrato (PaginaResponse). ADMIN ve todas; CLIENTE so as dele.
        // page ou size na URL pede a pagina (padrao do Spring Data); sem nenhum dos dois, a lista inteira.
        if (Paginacao.querPagina(page, size)) {
            Page<ContaResponseDto> pagina = contaService.listarPaginado(Paginacao.criar(page, size), usuario);
            return ResponseEntity.ok(PaginaResponse.de(pagina));
        }

        // Caso contrário, retorna lista completa.
        List<ContaResponseDto> resposta = contaService.listarTodas(usuario);
        return ResponseEntity.ok(resposta);
    }
}
