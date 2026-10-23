# Nomes e rotas: decisão de 22/10/2026

> **Status:** decidido; aplicado por script **depois** que as branches de 22/10 entrarem na `develop`
> (branch `refactor/nomes-e-rotas-de-mercado`, contrato **2.6**). O script foi validado numa prévia com a `develop` +
> as três branches: **261 testes**, frontend (lint, typecheck, build) e smoke **32/32** verdes.

## Critérios

1. **Domínio em português, termo técnico consagrado em inglês.** `ContaService`, `ViaCepClient`, `TratadorGlobalDeErros`.
2. **Sufixos `Exception`, `Enum` e `Dto` ficam** (decisão do time). Análise de mercado:
   - `Exception`: universal (JDK, Spring, *Effective Java*). Manter é o certo.
   - `Dto`: muito comum em Spring e no mercado brasileiro; projetos novos com `record` também usam só `Request`/`Response`. As duas formas são aceitas; o que importa é a consistência (e `Dto`, não `DTO`, como no Google Java Style).
   - `Enum`: o menos comum (o JDK e o Spring não usam: `DayOfWeek`, `HttpStatus`). É convenção válida do time; saber explicar a escolha conta.
3. **Pacotes de camada em inglês** (`controllers`, `services`, `repositories`, `clients`, `config`): é o padrão, e misturar um pacote em português no meio quebraria a consistência.
4. **Um nome, um significado:** `Cliente` é o correntista, então quem chama API externa é `Client` (`ViaCepClient`).
5. **Rotas REST:** substantivo no plural, verbo no método HTTP, o `me` do token em vez de id na URL quando o recurso é do próprio usuário.

## Classes e métodos

| Antes | Depois | Por quê |
|---|---|---|
| `ClienteViaCep` | `ViaCepClient` | "Cliente" já é o correntista; `XxxClient` é o padrão para integração |
| `ResponseViaCepDto` | `ViaCepResponseDto` | mesma ordem dos outros DTOs (`<Assunto>ResponseDto`) |
| `ControllerExceptionHandler` | `TratadorGlobalDeErros` | diz o que faz (o equivalente ao `GlobalExceptionHandler` do mercado) |
| `RespostaErroSeguranca` | `TratadorErrosSeguranca` | par do tratador global, para 401/403 |
| `CriadorAdmin` · `CriadorAcessoEquipe` | `InicializadorAdmin` · `InicializadorAcessoEquipe` | "inicializador" é o termo para o que roda uma vez na subida |
| `security.AcessoAlteradoException` | `exceptions.AcessoAlteradoException` | todas as exceções no mesmo pacote |
| `AuthService` | `AutenticacaoService` | linguagem do domínio (o controller segue `AuthController` por causa da rota `/auth/login`) |
| `NegocioException` (409) | `ConflitoException` | é usada para duplicidade, não para regra |
| `NaoProcessavelException` (422) | `RegraNegocioException` | o nome era o do status HTTP, não do problema |
| `ServicoExternoException` | `ServicoExternoIndisponivelException` | diz o que aconteceu (503) |
| `DepositarRequestDto` · `TransferirRequestDto` · `AbrirContaRequestDto` | `DepositoRequestDto` · `TransferenciaRequestDto` · `AberturaContaRequestDto` | DTO é coisa: substantivo, como o `SaqueRequestDto` |
| `TokenService.gerar` · `TokenGerado` | `emitir` · `TokenEmitido` | token é **emitido** (*issue*, no vocabulário do OAuth2) |
| `ClienteService.toResponse` · `getCliente` · `getCpfFormatado` | `paraResponse` · `montarCliente` · `formatarCpf` | `get` sugere *getter*; os outros services já usam `paraResponse` |
| `ContaController.listar` (extrato) · `listarContas` | `extrato` · `listar` | estava invertido para quem lê |
| `ClienteController.listarClientes` | `listar` | redundante dentro do `ClienteController` |
| `ContaController.semConteudo` · `assinatura` | `respostaSemCorpo` · `assinaturaDaOperacao` | `assinatura` lembrava assinatura digital |
| `UsuarioLogado.ehDonoDoCliente` | `ehDono` | o parâmetro já diz de quem |
| `RotasObsoletasFiltro` (novo) | `FiltroRotasObsoletas` | segue o `FiltroLogRequisicao` |

## Rotas (contrato 2.6, sem quebrar o frontend)

| Antes | Depois | Por quê |
|---|---|---|
| `POST /contas/clientes/{clienteId}` | `POST /contas` | o id da URL só servia para ser conferido contra o token |
| `POST /contas/{id}/deposito` · `/saque` | `/depositos` · `/saques` | recurso no plural; cada chamada cria um |
| `POST /contas/transferencia` | `POST /transferencias` | envolve duas contas: não pertence a uma |
| `PATCH /auth/senha` | `PATCH /usuarios/me/senha` | a senha é do usuário (cliente ou admin); par de `/admin/usuarios/{id}/senha` |
| `?paginado=true&page&size` | `?page&size` | padrão do Spring Data; a flag em português no meio de `page`/`size` em inglês sobrava |
| campo de entrada `tipoContaEnum` | aceita também `tipoConta` | o JSON não deveria contar como o Java guarda o dado |

As antigas **continuam funcionando** e respondem com `Deprecation: true` (RFC 9745) e `Link: <nova>;
rel="successor-version"` (RFC 8288). Saem só num contrato **3.0**, combinado com o Renan.

**Rotas que já estavam no padrão e ficam:** `POST /clientes`, `GET /clientes`, `GET`/`PATCH /clientes/me`,
`POST /auth/login`, `GET /contas`, `GET /contas/{id}/lancamentos`, `/admin/usuarios/**`, `GET /actuator/health`.
`POST /clientes/me/encerramento` fica como recurso de ação (como o `cancel` de APIs como a do Stripe) porque o
cadastro **não é apagado** na hora: um `DELETE` prometeria uma coisa que a lei não deixa cumprir.

## Para o contrato 3.0 (quebra o frontend, combinar antes)

- remover as rotas obsoletas e o parâmetro `paginado`;
- resposta com `tipoConta` no lugar de `tipoContaEnum`;
- versionar a API (`/v1/...`), o padrão de mercado para a próxima quebra não pegar ninguém de surpresa.
