package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.response.cliente.TermosResponseDto;
import com.br.rkfbank.services.TermosDeUso;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Publica (o cadastro ainda nao tem token): diz qual versao dos termos o POST /clientes espera em termosVersao.
@Tag(name = "Clientes", description = "cadastro público e listagem (ADMIN)")
@RestController
public class TermosController {

    private final TermosDeUso termosDeUso;

    public TermosController(TermosDeUso termosDeUso) {
        this.termosDeUso = termosDeUso;
    }

    @SecurityRequirements
    @GetMapping("/termos")
    public ResponseEntity<TermosResponseDto> vigentes() {
        return ResponseEntity.ok(new TermosResponseDto(termosDeUso.versaoVigente()));
    }
}
