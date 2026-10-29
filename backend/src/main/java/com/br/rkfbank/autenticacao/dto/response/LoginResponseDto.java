package com.br.rkfbank.autenticacao.dto.response;

import com.br.rkfbank.autenticacao.PerfilEnum;
import java.util.UUID;

// clienteId vai junto para o frontend saber quais contas sao do titular (o ADMIN ve todas); e nulo para o ADMIN do .env.
// deveTrocarSenha: senha temporaria (redefinida pelo ADMIN). O front leva direto para a troca; as outras rotas dao 403.
// refreshToken: renova a sessao em POST /auth/refresh sem pedir a senha (vale refreshExpiraEmSegundos, 7 dias). Uso unico:
// cada renovacao devolve outro. Guarde fora do alcance do JavaScript (o front usa um cookie httpOnly).
public record LoginResponseDto(
    String token,
    String tipo,
    long expiraEmSegundos,
    UUID clienteId,
    PerfilEnum perfil,
    boolean deveTrocarSenha,
    String refreshToken,
    long refreshExpiraEmSegundos
) {

    // O toString padrao do record imprimiria os dois tokens: com eles, qualquer um entra como o usuario.
    @Override
    public String toString() {
        return "LoginResponseDto[token=***, tipo=" + tipo + ", expiraEmSegundos=" + expiraEmSegundos + ", clienteId=" + clienteId
                + ", perfil=" + perfil + ", deveTrocarSenha=" + deveTrocarSenha + ", refreshToken=***]";
    }
}
