package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.comum.PaginaResponse;
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

    // Cadastro publico (sem token). @Valid dispara o Bean Validation do DTO antes de chamar o service.
    // 201 Created: o recurso foi criado, e o corpo traz o cliente como ficou salvo (sem a senha).
    @PostMapping
    public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
        // Camada HTTP: valida request, delega regra ao service e devolve 201 com corpo.
        log.debug("Recebido cadastro de cliente");
        return ResponseEntity.status(201).body(clienteService.cadastrar(request));
    }

    // So ADMIN (regra na ConfiguracaoSeguranca). Sem "paginado", devolve a lista inteira; com paginado=true,
    // uma pagina por vez. ResponseEntity<?> porque o corpo muda de formato conforme o parametro.
    @GetMapping
    public ResponseEntity<?> listarClientes(
            @RequestParam(defaultValue = "false") boolean paginado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Listagem de clientes (paginado={}, page={}, size={})", paginado, page, size);
        // Quando paginado=true, retorna a pagina no formato do contrato (PaginaResponse), nunca o Page do Spring.
        if (paginado) {
            Page<ClienteResponseDto> pagina = clienteService.listarPaginado(Paginacao.criar(page, size));
            return ResponseEntity.ok(PaginaResponse.de(pagina));
        }

        // Caso contrário, retorna lista completa para consumo simples.
        List<ClienteResponseDto> resposta = clienteService.listarTodos();
        return ResponseEntity.ok(resposta);
    }
}
