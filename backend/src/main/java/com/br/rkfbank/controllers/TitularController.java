package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.cliente.AtualizarClienteRequestDto;
import com.br.rkfbank.dto.request.cliente.EncerrarCadastroRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.dto.response.cliente.EncerramentoResponseDto;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.TitularService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Direitos do titular (LGPD): "me" = o cliente do token. Nao existe /clientes/{id} aqui de proposito: o id viria
// do usuario e seria mais uma porta para ver o cadastro de outra pessoa. Exige token (anyRequest().authenticated()).
@RestController
@RequestMapping("/clientes/me")
public class TitularController {

    private static final Logger log = LogManager.getLogger(TitularController.class);

    private final TitularService titularService;

    public TitularController(TitularService titularService) {
        this.titularService = titularService;
    }

    @GetMapping
    public ResponseEntity<ClienteResponseDto> meusDados(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Titular consultou os proprios dados");
        return ResponseEntity.ok(titularService.meusDados(UsuarioLogado.de(jwt)));
    }

    @PatchMapping
    public ResponseEntity<ClienteResponseDto> corrigir(@Valid @RequestBody AtualizarClienteRequestDto request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        log.debug("Titular pediu correcao do cadastro");
        return ResponseEntity.ok(titularService.corrigir(request, UsuarioLogado.de(jwt)));
    }

    // POST e nao DELETE: o cadastro nao some agora (fica guardado pelo prazo legal); e um pedido de encerramento.
    @PostMapping("/encerramento")
    public ResponseEntity<EncerramentoResponseDto> encerrar(@Valid @RequestBody EncerrarCadastroRequestDto request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        log.debug("Titular pediu o encerramento do cadastro");
        return ResponseEntity.ok(titularService.encerrar(request.senha(), UsuarioLogado.de(jwt)));
    }
}
