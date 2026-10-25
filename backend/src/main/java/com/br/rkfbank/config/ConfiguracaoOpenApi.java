package com.br.rkfbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger / OpenAPI (feature 4): documentacao VIVA da API, gerada do proprio codigo pelo springdoc.
// /v3/api-docs devolve a especificacao (JSON no padrao OpenAPI 3); /swagger-ui.html desenha a tela onde da para
// chamar cada rota. As rotas, os DTOs, os headers (Idempotency-Key) e os status de erro do TratadorGlobalDeErros
// entram sozinhos; aqui so o que o codigo nao diz: titulo, versao do contrato e como autenticar.
@Configuration
public class ConfiguracaoOpenApi {

    static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI apiRkfBank() {
        return new OpenAPI()
                .info(new Info()
                        .title("RKF Bank API")
                        .version("3.0")
                        .description("""
                                Banco digital do RKF Bank. Fluxo: `POST /clientes` (cadastro, público) → `POST /auth/login` \
                                (copie o `token`) → botão **Authorize** (cole só o token) → rotas de contas e dinheiro.
                                Depósito, saque e transferência exigem o header `Idempotency-Key` (um UUID novo por operação).
                                Todo erro traz `codigo` (estável) e `message` (para exibir). Contrato completo: \
                                `backend/README_CONTRATO_API.md`.""")
                        .license(new License().name("Projeto de estudo").url("https://github.com/k3vinrich4rd/rkf_bank")))
                // Bearer JWT: o Swagger UI guarda o token do Authorize e manda "Authorization: Bearer <token>" sozinho.
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("O token do POST /auth/login (vale 15 min; 60 nos perfis de teste).")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
