package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.auth.LoginRequestDto;
import com.br.rkfbank.dto.request.auth.TrocarSenhaRequestDto;
import com.br.rkfbank.dto.response.auth.LoginResponseDto;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// POST /auth/login e publica (ver ConfiguracaoSeguranca); PATCH /auth/senha exige token. O resultado sai no log do AutenticacaoService.
@RestController
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    // /usuarios/me/senha e a rota canonica desde o contrato 2.6: a senha e do USUARIO logado (cliente ou admin), par de
    // /admin/usuarios/{id}/senha. /auth/senha continua funcionando (obsoleta, com Deprecation) ate o contrato 3.0.
    // Exige token (so /auth/login e publica). 204: o token usado aqui tambem deixa de valer; entre de novo com a senha nova.
    @PatchMapping({"/usuarios/me/senha", "/auth/senha"})
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocarSenhaRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        autenticacaoService.trocarSenha(UsuarioLogado.de(jwt), request);
        return ResponseEntity.noContent().build();
    }

    // Rota publica (permitAll na ConfiguracaoSeguranca): e aqui que se consegue o token. O corpo nunca vai para o
    // log; o toString do LoginRequestDto esconde CPF e senha.
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(autenticacaoService.login(request));
    }
}
