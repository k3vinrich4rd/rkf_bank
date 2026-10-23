package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.services.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Unica rota publica alem do cadastro (permitAll na ConfiguracaoSeguranca): e aqui que se consegue o token.
// O resultado (sucesso, senha errada, bloqueio) sai no log do AutenticacaoService.
@RestController
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    // O corpo nunca vai para o log; o toString do LoginRequestDto esconde CPF e senha.
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(autenticacaoService.login(request));
    }
}
