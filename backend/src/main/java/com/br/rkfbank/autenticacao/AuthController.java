package com.br.rkfbank.autenticacao;

import com.br.rkfbank.autenticacao.dto.request.LoginRequestDto;
import com.br.rkfbank.autenticacao.dto.request.RefreshRequestDto;
import com.br.rkfbank.autenticacao.dto.response.LoginResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Rotas publicas da sessao (permitAll na ConfiguracaoSeguranca): entrar, renovar e sair. Renovar e sair nao pedem o
// token de acesso (ele pode ja ter vencido): usam o token de renovacao do corpo. O resultado sai no log do service.
@Tag(name = "Autenticação", description = "login com CPF e senha, renovação da sessão e saída (públicas)")
@RestController
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    // O corpo nunca vai para o log; o toString do LoginRequestDto esconde CPF e senha.
    @SecurityRequirements // publica: sem o cadeado do Authorize no Swagger
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(autenticacaoService.login(request));
    }

    // Troca o token de renovacao por um par novo (o usado deixa de valer). 401 SESSAO_EXPIRADA: entrar com a senha.
    @SecurityRequirements
    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDto> renovar(@Valid @RequestBody RefreshRequestDto request) {
        return ResponseEntity.ok(autenticacaoService.renovar(request.refreshToken()));
    }

    // Revoga o token de renovacao. Sempre 204 (sair duas vezes, ou com um token desconhecido, nao e erro).
    @SecurityRequirements
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> sair(@Valid @RequestBody RefreshRequestDto request) {
        autenticacaoService.sair(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
