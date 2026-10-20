package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.request.auth.TrocarSenhaRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// POST /auth/login e publica (ver ConfiguracaoSeguranca); PATCH /auth/senha exige token. O resultado sai no log do AuthService.
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Exige token (so /auth/login e publica). 204: o token usado aqui tambem deixa de valer; entre de novo com a senha nova.
    @PatchMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocarSenhaRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        authService.trocarSenha(UsuarioLogado.de(jwt), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
