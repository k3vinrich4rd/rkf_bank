package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.services.ClienteService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Metodo, rota, status e duracao de cada chamada ja saem no FiltroLogRequisicao; aqui so o detalhe em DEBUG.
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private static final Logger log = LogManager.getLogger(ClienteController.class);

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
        // Camada HTTP: valida request, delega regra ao service e devolve 201 com corpo.
        log.debug("Recebido cadastro de cliente");
        return ResponseEntity.status(201).body(clienteService.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<?> listarClientes(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Listagem de clientes (paginado={}, page={}, size={})", paginado, page, size);
        // Quando paginado=true, retorna Page com metadados de paginação.
        if (paginado) {
            Page<ClienteResponseDto> resposta = clienteService.listarPaginado(Paginacao.criar(page, size));
            return ResponseEntity.ok(resposta);
        }

        // Caso contrário, retorna lista completa para consumo simples.
        List<ClienteResponseDto> resposta = clienteService.listarTodos();
        return ResponseEntity.ok(resposta);
    }
}
