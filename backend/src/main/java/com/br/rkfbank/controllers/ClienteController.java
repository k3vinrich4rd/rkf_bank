package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.services.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
        // Camada HTTP: valida request, delega regra ao service e devolve 201 com corpo.
        return ResponseEntity.status(201).body(clienteService.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<?> listarClientes(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Quando paginado=true, retorna Page com metadados de paginação.
        if (paginado) {
            Page<ClienteResponseDto> resposta = clienteService.listarPaginado(PageRequest.of(page, size));
            return ResponseEntity.ok(resposta);
        }

        // Caso contrário, retorna lista completa para consumo simples.
        List<ClienteResponseDto> resposta = clienteService.listarTodos();
        return ResponseEntity.ok(resposta);
    }
}