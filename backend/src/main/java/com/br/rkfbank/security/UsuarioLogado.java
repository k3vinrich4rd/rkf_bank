package com.br.rkfbank.security;

import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.services.TokenService;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

// Quem esta chamando, lido do JWT ja validado (assinatura e exp conferidas pelo Resource Server).
// O controller monta e passa ao service: a regra de dono nao depende do Spring Security.
public record UsuarioLogado(UUID usuarioId, UUID clienteId, PerfilEnum perfil) {

    // Le as claims do token (sub, clienteId, perfil). O token ja foi validado; aqui so vira um objeto do dominio,
    // para o service receber "quem esta chamando" sem depender de classes do Spring Security.
    public static UsuarioLogado de(Jwt jwt) {
        String clienteId = jwt.getClaimAsString(TokenService.CLAIM_CLIENTE_ID);
        return new UsuarioLogado(
                UUID.fromString(jwt.getSubject()),
                clienteId != null ? UUID.fromString(clienteId) : null,
                PerfilEnum.valueOf(jwt.getClaimAsString(TokenService.CLAIM_PERFIL))
        );
    }

    public boolean isAdmin() {
        return perfil == PerfilEnum.ADMIN;
    }

    // ADMIN nao tem clienteId, entao nunca e dono de conta.
    public boolean ehDono(UUID idDoCliente) {
        return clienteId != null && clienteId.equals(idDoCliente);
    }
}
