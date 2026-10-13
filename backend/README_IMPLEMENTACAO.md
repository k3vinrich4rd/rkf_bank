# RKF Bank — Arquitetura e implementação do backend

Este documento explica **como o backend é organizado, por que foi feito assim** e **como adicionar código novo
sem bagunçar a estrutura**. Para rotas e payloads, veja o [contrato](README_CONTRATO_API.md).

---

## 1. Visão geral

Padrão adotado: **arquitetura em camadas** (*Layered Architecture*).

```mermaid
flowchart TB
    REQ([Requisição HTTP]) --> CTRL
    subgraph Aplicação
        CTRL["controllers<br/>recebe HTTP, valida DTO, devolve status"]
        SRV["services<br/>regras de negócio e transações"]
        REPO["repositories<br/>acesso ao banco (Spring Data JPA)"]
        CLI["clients/viacep<br/>integração externa"]
        HND["controllers/handlers<br/>exceção → resposta de erro"]
    end
    CTRL --> SRV
    SRV --> REPO
    SRV --> CLI
    REPO --> DB[(MySQL)]
    CLI --> VIA[[ViaCEP]]
    SRV -. lança exceção .-> HND
    HND --> RESP([Resposta de erro padronizada])
```

**Regra de dependência:** cada camada só conhece a de baixo. Controller não acessa repository; repository não
conhece DTO; entidade JPA nunca sai pela API.

**Por que camadas (e não hexagonal, microsserviços etc.)?** O sistema é pequeno, o time tem duas pessoas e o
prazo é curto. Camadas são simples, conhecidas por qualquer dev Spring e facilitam testar cada parte isolada.
Quando o domínio crescer (Pix, cartão), dá para evoluir para pacotes por funcionalidade — ver seção 7.

---

## 2. Mapa de pacotes

Raiz: `backend/src/main/java/com/br/rkfbank/`

| Pacote | Responsabilidade | Pode | Não pode |
|---|---|---|---|
| `controllers` | contrato HTTP: rota, `@Valid`, status, query params | chamar services | ter regra de negócio, acessar repository |
| `controllers/handlers` | traduzir exceção em status + `ErroResponse` | — | ter regra de negócio |
| `services` | regras de negócio, orquestração, `@Transactional` | chamar repositories, clients e outros services | conhecer `HttpServletRequest`, `ResponseEntity` |
| `repositories` | consultas ao banco | métodos derivados (`existsBy...`, `findBy...`) e `@Query` | ter regra de negócio |
| `entities` (+ `enums`) | modelo persistido (tabelas) | anotações JPA | ser devolvida na API |
| `dto/request` | o que **entra** na API, com Bean Validation | `@NotBlank`, `@Pattern`, `@CpfValido`… | lógica |
| `dto/response` | o que **sai** da API | — | expor dados internos (ex.: `versao`) |
| `dto/comum` | formato padrão de erro | — | — |
| `clients/viacep` | chamar o ViaCEP e traduzir falhas em exceções nossas | `RestClient`, timeouts | regra de negócio de cliente |
| `exceptions` | exceções de domínio (cada uma mapeia um status) | — | — |
| `validation` | validações customizadas (`@CpfValido`) | — | — |
| `config` | beans de configuração (CORS) | — | — |

---

## 3. Caminho de uma requisição

Exemplo: **transferência** (`POST /contas/transferencia`).

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant C as ContaController
    participant S as ContaService
    participant R as ContaRepository
    participant L as LancamentoRepository
    participant H as ExceptionHandler

    F->>C: POST /contas/transferencia {origem, destino, valor}
    Note over C: @Valid no TransferirRequestDto<br/>(falhou → 400 com campos)
    C->>S: transferir(request)
    Note over S: @Transactional — tudo ou nada
    S->>S: origem == destino? → 422
    S->>R: findById(origem) / findById(destino)
    R-->>S: contas (não existe → 404, inativa → 422)
    S->>S: saldo < valor? → 422
    S->>R: save(origem - valor), save(destino + valor)
    S->>L: save(Lancamento TRANSFERENCIA)
    S-->>C: void
    C-->>F: 204 No Content
    Note over S,H: Qualquer exceção sobe até o handler,<br/>a transação é desfeita (rollback)<br/>e o front recebe o ErroResponse
```

---

## 4. Como cada camada foi implementada

### 4.1 Controller — fino, só HTTP

[`ClienteController.java`](src/main/java/com/br/rkfbank/controllers/ClienteController.java)

```java
@PostMapping
public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
    return ResponseEntity.status(201).body(clienteService.cadastrar(request));
}
```

- `@Valid` dispara as anotações do DTO **antes** de chegar no service.
- Listagens aceitam `paginado`, `page` e `size`; a validação de limites (`page >= 0`, `1 <= size <= 100`)
  fica no próprio controller porque é regra de **HTTP**, não de negócio.

### 4.2 DTOs — `record` com validação

[`CadastroClienteRequestDto.java`](src/main/java/com/br/rkfbank/dto/request/cliente/CadastroClienteRequestDto.java)

```java
public record CadastroClienteRequestDto(
        @NotBlank(message = "cpf e obrigatório")
        @CpfValido(message = "cpf inválido")
        String cpf,
        ...
) {}
```

**Por que `record`?** É imutável, sem boilerplate e deixa claro que DTO é só dado.
**Por que não devolver a entidade?** A entidade tem campos internos (`versao`, relacionamentos), e qualquer
mudança no banco quebraria o frontend. O DTO é o contrato; a entidade é detalhe de implementação.

### 4.3 Service — onde mora a regra

[`ContaService.java`](src/main/java/com/br/rkfbank/services/ContaService.java)

```java
@Transactional
public void sacar(UUID contaId, SaqueRequestDto request) {
    Conta conta = buscarAtiva(contaId);              // 404 ou 422
    BigDecimal valor = validarValor(request.valor()); // > 0, 2 casas

    if (conta.getSaldo().compareTo(valor) < 0) {
        throw new NaoProcessavelException("Saldo insuficiente"); // 422
    }

    conta.setSaldo(conta.getSaldo().subtract(valor));
    contaRepository.save(conta);
    salvarLancamento(TipoLancamentoEnum.SAQUE, valor, conta.getId(), null, request.descricao());
}
```

Pontos de atenção que valem estudo:

| Técnica | Onde | Por quê |
|---|---|---|
| `BigDecimal` para dinheiro | `Conta.saldo`, `Lancamento.valor` | `double` erra conta (`0.1 + 0.2 != 0.3`) |
| `compareTo` em vez de `equals` | comparação de saldo | `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` é `false` |
| `setScale(2, HALF_EVEN)` | `validarValor` | arredondamento bancário, padrão em sistemas financeiros |
| `@Transactional` | todo método que escreve | débito + crédito + lançamento: tudo ou nada |
| `@Transactional(readOnly = true)` | listagens | otimização e intenção explícita |
| `@Version` (lock otimista) | `Conta.versao` | dois saques simultâneos não "somem" um com o outro → `409` |
| checar unicidade **e** capturar `DataIntegrityViolationException` | `cadastrar`, `abrir` | o `exists` dá mensagem boa; o `catch` cobre a corrida entre duas requisições |
| `saveAndFlush` | cadastro de cliente e conta | força o `INSERT` dentro do `try`, para o `catch` acima funcionar |
| sanitizar antes de validar no banco | CPF, telefone, CEP | `529.982.247-25` e `52998224725` são o mesmo CPF |

### 4.4 Repository — Spring Data deriva a query pelo nome

[`LancamentoRepository.java`](src/main/java/com/br/rkfbank/repositories/LancamentoRepository.java)

```java
List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);
```

Vira: `WHERE conta_origem_id = ? OR conta_destino_id = ? ORDER BY data_hora DESC`.

### 4.5 Entidades

| Entidade | Destaques |
|---|---|
| `Cliente` | `cpf` e `email` com `UniqueConstraint`; `Endereco` 1:1 obrigatório com `cascade = ALL`; 1:N com `Conta` |
| `Endereco` | dados vindos do ViaCEP + `numero`/`complemento` do usuário |
| `Conta` | `saldo` `DECIMAL(19,2)`; `tipoContaEnum` salvo como texto; `@Version versao` |
| `Lancamento` | guarda só os **IDs** das contas (sem FK); `dataHora` preenchida no `@PrePersist` |

Enums são salvos como `VARCHAR` (`EnumType.STRING`), não como número nem `ENUM` nativo do MySQL —
assim, reordenar ou adicionar valores não corrompe dados antigos.

### 4.6 Integração ViaCEP

[`ClienteViaCep.java`](src/main/java/com/br/rkfbank/clients/viacep/ClienteViaCep.java)

| Situação | O que acontece | Status final |
|---|---|---|
| CEP encontrado | devolve `ResponseViaCepDto` | — |
| ViaCEP responde `{"erro": true}` (com HTTP 200!) | `NaoProcessavelException("CEP não encontrado")` | `422` |
| ViaCEP responde 5xx | `ServicoExternoException("ViaCEP indisponível")` | `503` |
| timeout (3 s), DNS, rede | `ServicoExternoException("Falha ao consultar ViaCEP")` | `503` |

Lição: **serviço externo sempre falha em algum momento.** Timeout configurado + tradução de erro evitam que
uma API de terceiros derrube a nossa.

### 4.7 Tratamento de erros centralizado

[`ControllerExceptionHandler.java`](src/main/java/com/br/rkfbank/controllers/handlers/ControllerExceptionHandler.java)
(`@RestControllerAdvice`) transforma exceções em `ErroResponse`:

| Exceção | Status | Quando usar ao criar código novo |
|---|---|---|
| `MethodArgumentNotValidException` | `400` | automática, vem do `@Valid` |
| `HttpMessageNotReadableException` | `400` | automática, JSON inválido |
| `MethodArgumentTypeMismatchException` | `400` | automática, UUID inválido na URL |
| `RequisicaoInvalidaException(campo, msg)` | `400` | validação manual que precisa apontar um campo |
| `ClienteNaoEncontradoException` / `ContaNaoEncontradaException` | `404` | recurso não existe |
| `NegocioException(msg)` | `409` | **conflito**: algo já existe (CPF, e-mail, número de conta) |
| `OptimisticLockingFailureException` | `409` | automática, concorrência |
| `NaoProcessavelException(msg)` | `422` | **regra de negócio violada** (saldo, conta inativa, CEP) |
| `ServicoExternoException(msg)` | `503` | dependência externa falhou |

> 💡 O nome `NegocioException` engana: ela significa **conflito (409)**, não "qualquer regra de negócio".
> Para regra violada, use `NaoProcessavelException` (422).

### 4.8 Validação customizada `@CpfValido`

- Anotação: [`validation/CpfValido.java`](src/main/java/com/br/rkfbank/validation/CpfValido.java)
- Lógica: [`validation/CpfValidoValidator.java`](src/main/java/com/br/rkfbank/validation/CpfValidoValidator.java)
  — remove máscara, exige 11 dígitos, rejeita todos iguais (`111.111.111-11`) e confere os dois dígitos verificadores.
- Valor vazio retorna `true` de propósito: quem cuida de "obrigatório" é o `@NotBlank`. Cada anotação, uma responsabilidade.

---

## 5. Passo a passo: criando um endpoint novo

Exemplo real do roadmap: **`GET /contas/{contaId}`** (tela de saldo).

1. **Contrato** — mova o endpoint da seção 7 para a seção 4 do [contrato](README_CONTRATO_API.md), com
   resposta e erros. Avise o frontend.
2. **DTO** — reaproveite `ContaResponseDto` (se precisar de campos novos, crie outro record em `dto/response/conta`).
3. **Service** — em `ContaService`:

   ```java
   @Transactional(readOnly = true)
   public ContaResponseDto buscarPorId(UUID contaId) {
       return contaRepository.findById(contaId)
               .map(this::paraContaResponse)
               .orElseThrow(ContaNaoEncontradaException::new);
   }
   ```

   > Decisão para registrar: conta inativa deve aparecer (só leitura) ou dar `422`? Pergunte antes de codar.

4. **Controller** — em `ContaController`:

   ```java
   @GetMapping("/{contaId}")
   public ResponseEntity<ContaResponseDto> buscar(@PathVariable UUID contaId) {
       return ResponseEntity.ok(contaService.buscarPorId(contaId));
   }
   ```

5. **Testes**
   - `ContaServiceTest`: encontra a conta; lança `ContaNaoEncontradaException` quando não existe.
   - `ContaControllerTest`: devolve `200` com o corpo do service.
6. **Rodar** `./mvnw clean verify` e testar na mão (ver [README_TESTES_E2E.md](README_TESTES_E2E.md)).
7. **Frontend** — adicionar `buscar(contaId)` em `frontend/lib/api/contas.ts` (o Renan ou você, no mesmo PR
   se combinado).
8. **PR** com o checklist do [CONTRIBUTING.md](../CONTRIBUTING.md).

---

## 6. Testes

Os testes ficam em `backend/src/test/java/com/br/rkfbank/`, espelhando os pacotes do código.

| Camada | Como testamos | Exemplo |
|---|---|---|
| Validação | instancia o validator e chama `isValid` | `CpfValidoValidatorTest` |
| Client externo | mock do `RestClient` | `ClienteViaCepTest` |
| Service | `@ExtendWith(MockitoExtension.class)`, mocks dos repositories | `ContaServiceTest` |
| Controller | chama o método do controller com service mockado | `ContaControllerTest` |
| Handler | chama o handler com a exceção e confere status/corpo | `ControllerExceptionHandlerTest` |
| Aplicação | `@SpringBootTest` (sobe o contexto inteiro) | `RkfbankApplicationTests` |

**O que um bom teste de service cobre:** caminho feliz, cada `throw` (um teste por regra) e o que foi salvo
(use `ArgumentCaptor` para conferir o `Lancamento` gerado).

**Próximo nível** (ver [débitos técnicos](../planing/debitos-tecnicos.md)): testes de controller com `MockMvc`
(testam JSON e status de verdade) e testes de integração com Testcontainers (MySQL real).

---

## 7. Evolução planejada

| Hoje | Próximo passo | Por quê |
|---|---|---|
| `ddl-auto=update` | **Flyway** com migrations versionadas | histórico do banco no Git; deploy previsível |
| Sem documentação gerada | **springdoc-openapi** (Swagger UI) | contrato navegável e sempre atualizado |
| Sem autenticação | **Spring Security + JWT**, senha com BCrypt | qualquer um consegue mexer em qualquer conta |
| `System.out` do Hibernate | **logs estruturados** com id por requisição | rastrear uma operação de ponta a ponta |
| Pacotes por camada | pacotes por funcionalidade (`cliente/`, `conta/`) | quando entrar Pix e cartão |

A lista completa, com prioridade, está em [planing/debitos-tecnicos.md](../planing/debitos-tecnicos.md).
