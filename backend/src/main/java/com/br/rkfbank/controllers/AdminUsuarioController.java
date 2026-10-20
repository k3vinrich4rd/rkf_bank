package com.br.rkfbank.controllers;

import com.br.rkfbank.dto.request.usuario.AlterarAcessoRequestDto;
import com.br.rkfbank.dto.request.usuario.AlterarPerfilRequestDto;
import com.br.rkfbank.dto.request.usuario.RedefinirSenhaRequestDto;
import com.br.rkfbank.dto.response.usuario.UsuarioAdminResponseDto;
import com.br.rkfbank.security.UsuarioLogado;
import com.br.rkfbank.services.GestaoUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// So ADMIN (ConfiguracaoSeguranca: /admin/** -> hasRole("ADMIN")). Sem tela no frontend: usa-se pelo Postman/curl.
@RestController
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final GestaoUsuarioService gestaoUsuarioService;

    public AdminUsuarioController(GestaoUsuarioService gestaoUsuarioService) {
        this.gestaoUsuarioService = gestaoUsuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponseDto>> listar() {
        return ResponseEntity.ok(gestaoUsuarioService.listar());
    }

    @PatchMapping("/{usuarioId}/perfil")
    public ResponseEntity<UsuarioAdminResponseDto> alterarPerfil(@PathVariable UUID usuarioId,
                                                                 @Valid @RequestBody AlterarPerfilRequestDto request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gestaoUsuarioService.alterarPerfil(usuarioId, request.perfil(), UsuarioLogado.de(jwt)));
    }

    @PatchMapping("/{usuarioId}/acesso")
    public ResponseEntity<UsuarioAdminResponseDto> alterarAcesso(@PathVariable UUID usuarioId,
                                                                 @Valid @RequestBody AlterarAcessoRequestDto request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(gestaoUsuarioService.alterarAcesso(usuarioId, request.ativo(), UsuarioLogado.de(jwt)));
    }

    // 204: a senha nao volta na resposta. Os tokens da pessoa caem na hora; ela entra com a senha nova.
    @PatchMapping("/{usuarioId}/senha")
    public ResponseEntity<Void> redefinirSenha(@PathVariable UUID usuarioId, @Valid @RequestBody RedefinirSenhaRequestDto request,
                                               @AuthenticationPrincipal Jwt jwt) {
        gestaoUsuarioService.redefinirSenha(usuarioId, request.novaSenha(), UsuarioLogado.de(jwt));
        return ResponseEntity.noContent().build();
    }
}
