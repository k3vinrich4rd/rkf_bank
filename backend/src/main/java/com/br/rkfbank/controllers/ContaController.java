package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.services.ContaService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Metodo, rota, status e duracao de cada chamada ja saem no FiltroLogRequisicao; aqui so o detalhe em DEBUG.
@RestController
@RequestMapping("/contas")
public class ContaController {

    private static final Logger log = LogManager.getLogger(ContaController.class);

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping("/clientes/{clienteId}")
    public ResponseEntity<ContaResponseDto> abrir(@PathVariable UUID clienteId, @Valid @RequestBody AbrirContaRequestDto request) {
        // Criação de conta vinculada ao cliente com retorno 201.
        log.debug("Recebida abertura de conta {} para cliente {}", request.tipoContaEnum(), clienteId);
        return ResponseEntity.status(201).body(contaService.abrir(clienteId, request));
    }

    @PostMapping("/{contaId}/deposito")
    public ResponseEntity<Void> depositar(@PathVariable UUID contaId, @Valid @RequestBody DepositarRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        log.debug("Recebido deposito de {} na conta {}", request.valor(), contaId);
        contaService.depositar(contaId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{contaId}/saque")
    public ResponseEntity<Void> sacar(@PathVariable UUID contaId, @Valid @RequestBody SaqueRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        log.debug("Recebido saque de {} na conta {}", request.valor(), contaId);
        contaService.sacar(contaId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transferencia")
    public ResponseEntity<Void> transferir(@Valid @RequestBody TransferirRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        log.debug("Recebida transferencia de {} da conta {} para {}",
                request.valor(), request.contaOrigemId(), request.contaDestinoId());
        contaService.transferir(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contaId}/lancamentos")
    public ResponseEntity<List<LancamentoResponseDto>> listar(@PathVariable UUID contaId) {
        // Retorna extrato da conta em ordem decrescente por data.
        log.debug("Consulta de extrato da conta {}", contaId);
        return ResponseEntity.ok(contaService.listarLancamentos(contaId));
    }

    @GetMapping
    public ResponseEntity<?> listarContas(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Listagem de contas (paginado={}, page={}, size={})", paginado, page, size);
        // Quando paginado=true, retorna Page com metadados de paginação.
        if (paginado) {
            Page<ContaResponseDto> resposta = contaService.listarPaginado(Paginacao.criar(page, size));
            return ResponseEntity.ok(resposta);
        }

        // Caso contrário, retorna lista completa.
        List<ContaResponseDto> resposta = contaService.listarTodas();
        return ResponseEntity.ok(resposta);
    }
}
