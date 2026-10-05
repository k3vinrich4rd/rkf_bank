package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.services.ContaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

@RestController
@RequestMapping("/api")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping("/clientes/{clienteId}/contas")
    public ResponseEntity<ContaResponseDto> abrir(@PathVariable UUID clienteId, @Valid @RequestBody AbrirContaRequestDto request) {
        // Criação de conta vinculada ao cliente com retorno 201.
        return ResponseEntity.status(201).body(contaService.abrir(clienteId, request));
    }

    @PostMapping("/contas/{contaId}/deposito")
    public ResponseEntity<Void> depositar(@PathVariable UUID contaId, @Valid @RequestBody DepositarRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        contaService.depositar(contaId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contas/{contaId}/saque")
    public ResponseEntity<Void> sacar(@PathVariable UUID contaId, @Valid @RequestBody SaqueRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        contaService.sacar(contaId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transferencias")
    public ResponseEntity<Void> transferir(@Valid @RequestBody TransferirRequestDto request) {
        // Operação sem payload de saída: apenas confirma 204.
        contaService.transferir(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/contas/{contaId}/lancamentos")
    public ResponseEntity<List<LancamentoResponseDto>> listar(@PathVariable UUID contaId) {
        // Retorna extrato da conta em ordem decrescente por data.
        return ResponseEntity.ok(contaService.listarLancamentos(contaId));
    }

    @GetMapping("/contas")
    public ResponseEntity<?> listarContas(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Quando paginado=true, retorna Page com metadados de paginação.
        if (paginado) {
            Page<ContaResponseDto> resposta = contaService.listarPaginado(PageRequest.of(page, size));
            return ResponseEntity.ok(resposta);
        }

        // Caso contrário, retorna lista completa.
        List<ContaResponseDto> resposta = contaService.listarTodas();
        return ResponseEntity.ok(resposta);
    }
}