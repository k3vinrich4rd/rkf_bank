package com.br.rkfbank.config;

import com.br.rkfbank.services.IdempotenciaService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
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
                        .version("3.1")
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

    // O "Try it out" do Swagger UI manda o exemplo do schema como corpo. Sem exemplo, o login ia com
    // {"cpf":"string","senha":"string"} e o deposito sem Idempotency-Key: a tela abria, mas nada funcionava sem adivinhar.
    // Os exemplos usam so as contas PUBLICAS de visitante (Ana e Bruno, VISITANTES.md): no perfil visitante, e so clicar.
    @Bean
    public OpenApiCustomizer exemplosParaOTryItOut() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            EXEMPLOS.forEach((nome, exemplo) -> {
                Schema schema = schemas.get(nome);
                if (schema != null) {
                    schema.setExample(exemplo);
                }
            });
            // O springdoc anuncia 200 em toda rota; estas respondem 201 (criou) ou 204 (deu certo, sem corpo), como no
            // contrato. Sem isso, o Swagger mostra o 204 de verdade como "Undocumented".
            openApi.getPaths().forEach((caminho, rota) -> rota.readOperationsMap().forEach((metodo, operacao) -> {
                String sucesso = STATUS_DE_SUCESSO.get(metodo + " " + caminho);
                ApiResponse padrao = operacao.getResponses().remove("200");
                if (sucesso == null || padrao == null) {
                    if (padrao != null) {
                        operacao.getResponses().addApiResponse("200", padrao);
                    }
                    return;
                }
                if ("204".equals(sucesso)) {
                    padrao.setContent(null);
                }
                padrao.setDescription("204".equals(sucesso) ? "Deu certo (sem corpo)" : "Criado");
                operacao.getResponses().addApiResponse(sucesso, padrao);
            }));
            openApi.getPaths().values().forEach(rota -> rota.readOperations().forEach(operacao -> {
                if (operacao.getParameters() == null) {
                    return;
                }
                operacao.getParameters().forEach(parametro -> {
                    // O controller recebe o header como opcional para o TratadorGlobalDeErros responder o 400 no formato
                    // padrao; para quem le o Swagger, ele e obrigatorio (sem ele, toda movimentacao da 400).
                    if ("header".equals(parametro.getIn()) && IdempotenciaService.HEADER.equals(parametro.getName())) {
                        parametro.setRequired(true);
                        parametro.setDescription("Um UUID NOVO por operação (ex.: rode `node -e \"console.log(crypto.randomUUID())\"`). "
                                + "Repetir a mesma chave com o mesmo corpo não movimenta de novo (204 com Idempotency-Replayed: true); "
                                + "com outro corpo, 422 IDEMPOTENCIA_CHAVE_REUSADA.");
                        parametro.setExample("3f1c2a9e-8b7d-4c6e-9a51-0d2b7e4f6a10");
                    }
                    if ("path".equals(parametro.getIn()) && "contaId".equals(parametro.getName())) {
                        parametro.setDescription("Id da conta (o `id` de GET /contas). Exemplo: a conta da Ana Visitante.");
                        parametro.setExample(CONTA_ANA);
                    }
                });
            }));
        };
    }

    private static final Map<String, String> STATUS_DE_SUCESSO = Map.of(
            "POST /clientes", "201",
            "POST /contas", "201",
            "POST /contas/{contaId}/depositos", "204",
            "POST /contas/{contaId}/saques", "204",
            "POST /transferencias", "204",
            "POST /auth/logout", "204",
            "PATCH /usuarios/me/senha", "204",
            "PATCH /admin/usuarios/{usuarioId}/senha", "204");

    private static final String CONTA_ANA = "a0000000-0000-4000-8000-000000000114";
    private static final String CONTA_BRUNO = "a0000000-0000-4000-8000-000000000124";

    private static final Map<String, Map<String, Object>> EXEMPLOS = Map.ofEntries(
            Map.entry("LoginRequestDto", exemplo("cpf", "246.813.579-28", "senha", "Visitante@2026")),
            Map.entry("RefreshRequestDto", exemplo("refreshToken", "cole aqui o refreshToken do login")),
            Map.entry("CadastroClienteRequestDto", exemplo(
                    "nomeCompleto", "Cliente do Swagger", "cpf", "529.982.247-25", "email", "cliente.swagger@exemplo.test",
                    "telefone", "(11) 99999-8888", "cep", "06020-010", "numero", "123", "complemento", "Apto 45",
                    "senha", "senha-do-swagger-1", "aceiteTermos", true)),
            Map.entry("AberturaContaRequestDto", exemplo("tipoConta", "POUPANCA")),
            Map.entry("DepositoRequestDto", exemplo("valor", 150.00, "descricao", "Depósito pelo Swagger")),
            Map.entry("SaqueRequestDto", exemplo("valor", 50.00, "descricao", "Saque pelo Swagger")),
            Map.entry("TransferenciaRequestDto", exemplo("contaOrigemId", CONTA_ANA, "contaDestinoId", CONTA_BRUNO,
                    "valor", 25.00, "descricao", "Transferência pelo Swagger")),
            Map.entry("AtualizarClienteRequestDto", exemplo("telefone", "(11) 91234-5678")),
            Map.entry("TrocarSenhaRequestDto", exemplo("senhaAtual", "Visitante@2026", "novaSenha", "nova-senha-123")),
            Map.entry("EncerrarCadastroRequestDto", exemplo("senha", "a senha de quem está logado")),
            Map.entry("RedefinirSenhaRequestDto", exemplo("novaSenha", "temporaria-123")),
            Map.entry("AlterarPerfilRequestDto", exemplo("perfil", "ADMIN")),
            Map.entry("AlterarAcessoRequestDto", exemplo("ativo", false)));

    // Ordem dos campos preservada: o Swagger mostra o corpo na mesma ordem do contrato.
    private static Map<String, Object> exemplo(Object... chavesEValores) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            mapa.put((String) chavesEValores[i], chavesEValores[i + 1]);
        }
        return mapa;
    }
}
