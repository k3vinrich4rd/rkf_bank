package com.br.rkfbank.autenticacao;

import com.br.rkfbank.autenticacao.dto.request.TrocarSenhaRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// O usuario logado cuidando do proprio acesso ("me" = o usuario do token, cliente ou ADMIN).
// Par de /admin/usuarios/{id}, onde o ADMIN cuida do acesso dos outros. Exige token.
@Tag(name = "Usuário logado", description = "o próprio acesso: troca de senha")
@RestController
@RequestMapping("/usuarios/me")
public class UsuarioController {

    private final AutenticacaoService autenticacaoService;

    public UsuarioController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    // 204: a senha nao volta. Todo token antigo (inclusive o usado aqui) deixa de valer; entre de novo com a nova.
    @PatchMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocarSenhaRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        autenticacaoService.trocarSenha(UsuarioLogado.de(jwt), request);
        return ResponseEntity.noContent().build();
    }
}
