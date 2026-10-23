package com.br.rkfbank.exceptions;

import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

// Token com assinatura valida, mas que nao vale mais: o usuario foi desativado, apagado ou mudou de perfil depois do
// login. Sai como 401 pelo TratadorErrosSeguranca, com mensagem propria para quem testa entender o motivo.
public class AcessoAlteradoException extends InvalidBearerTokenException {

    public AcessoAlteradoException() {
        super("Acesso do usuario alterado depois do login");
    }
}
