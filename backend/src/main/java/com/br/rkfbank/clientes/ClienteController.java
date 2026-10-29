package com.br.rkfbank.clientes;

import com.br.rkfbank.clientes.dto.request.CadastroClienteRequestDto;
import com.br.rkfbank.clientes.dto.response.ClienteResponseDto;
import com.br.rkfbank.compartilhado.web.PaginaResponse;
import com.br.rkfbank.compartilhado.web.Paginacao;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// Metodo, rota, status e duracao de cada chamada ja saem no FiltroLogRequisicao; aqui so o detalhe em DEBUG.
@Tag(name = "Clientes", description = "cadastro público e listagem (ADMIN)")
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
    @SecurityRequirements // publica: sem o cadeado do Authorize no Swagger
    @PostMapping
    public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
        // Camada HTTP: valida request, delega regra ao service e devolve 201 com corpo.
        log.debug("Recebido cadastro de cliente");
        return ResponseEntity.status(201).body(clienteService.cadastrar(request));
    }

    // So ADMIN (regra na ConfiguracaoSeguranca). Sem page/size na URL, devolve a lista inteira; com page ou size,
    // uma pagina por vez. ResponseEntity<?> porque o corpo muda de formato conforme os parametros.
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.debug("Listagem de clientes (page={}, size={})", page, size);
        // A pagina sai no formato do contrato (PaginaResponse), nunca o Page do Spring.
        if (Paginacao.querPagina(page, size)) {
            Page<ClienteResponseDto> pagina = clienteService.listarPaginado(Paginacao.criar(page, size));
            return ResponseEntity.ok(PaginaResponse.de(pagina));
        }

        // Caso contrário, retorna lista completa para consumo simples.
        List<ClienteResponseDto> resposta = clienteService.listarTodos();
        return ResponseEntity.ok(resposta);
    }
}
