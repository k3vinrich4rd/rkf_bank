# RKF Bank - README de implementacao (versao final)

## 1) Tecnologias usadas

- Java 21 (LTS)
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA + Hibernate
- Jakarta Validation
- RestClient (ViaCEP)
- MySQL 8.4 (Docker Compose)
- Maven Wrapper
- JaCoCo

## 2) Arquitetura oficial do projeto

Padrao adotado: **Layered Architecture (Arquitetura em Camadas)**.

- `controllers`: contrato HTTP (`ResponseEntity`, status, query params)
- `services`: regra de negocio e controle transacional
- `repositories`: persistencia com Spring Data JPA
- `entities`: estado do dominio no banco
- `dto/request` e `dto/response`: contrato externo da API
- `dto/comum`: payload padrao de erro
- `clients/viacep`: integracao externa isolada
- `controllers/handlers`: tradutor de excecao para HTTP
- `validation`: validacao customizada (`@CpfValido`)

Motivo da escolha:

- encaixa no tamanho atual do sistema
- separa responsabilidade por camada
- facilita manutencao e testes isolados

## 3) Como cada camada foi implementada (trecho real)

### 3.1 Controller (entrada HTTP)

Responsabilidade: receber request, validar payload e delegar para service.

Arquivo: `backend/src/main/java/com/br/rkfbank/controllers/ClienteController.java`

```java
@PostMapping
public ResponseEntity<ClienteResponseDto> cadastrar(@Valid @RequestBody CadastroClienteRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.cadastrar(request));
}
```

Arquivo: `backend/src/main/java/com/br/rkfbank/controllers/ContaController.java`

```java
@GetMapping("/contas")
public ResponseEntity<?> listarContas(
        @RequestParam(defaultValue = "false") boolean paginado,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    if (paginado) {
        return ResponseEntity.ok(contaService.listarPaginado(PageRequest.of(page, size)));
    }
    return ResponseEntity.ok(contaService.listarTodas());
}
```

### 3.2 Service (regra de negocio)

Responsabilidade: validar regra, orquestrar repositorios e integracoes, retornar DTO.

Arquivo: `backend/src/main/java/com/br/rkfbank/services/ClienteService.java`

```java
if (clienteRepository.existsByCpf(request.cpf())) {
    throw new NegocioException("CPF ja cadastrado");
}
String cep = request.cep().replaceAll("\\D", "");
ResponseViaCepDto viaCep = clienteViaCep.buscarPorCep(cep);
Cliente salvo = clienteRepository.save(cliente);
return paraResponse(salvo);
```

Arquivo: `backend/src/main/java/com/br/rkfbank/services/ContaService.java`

```java
if (request.contaOrigemId().equals(request.contaDestinoId())) {
    throw new NaoProcessavelException("Conta de origem e destino nao podem ser iguais");
}
if (origem.getSaldo().compareTo(valor) < 0) {
    throw new NaoProcessavelException("Saldo insuficiente para transferencia");
}
```

### 3.3 Repository (persistencia)

Responsabilidade: consultas derivadas e acesso ao banco.

Arquivo: `backend/src/main/java/com/br/rkfbank/repositories/ContaRepository.java`

```java
public interface ContaRepository extends JpaRepository<Conta, UUID> {
    boolean existsByNumeroConta(String numeroConta);
}
```

Arquivo: `backend/src/main/java/com/br/rkfbank/repositories/LancamentoRepository.java`

```java
List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);
```

### 3.4 Entities e enums (dominio)

Responsabilidade: modelo de banco e semantica de negocio.

- `Conta.saldo`: `BigDecimal`
- `Conta.tipoContaEnum`: `@Enumerated(EnumType.STRING)`
- `Lancamento.tipoLancamentoEnum`: `DEPOSITO`, `SAQUE`, `TRANSFERENCIA`

### 3.5 Integracao ViaCEP (client externo)

Responsabilidade: encapsular chamada HTTP e mapear falhas para excecoes de dominio.

Arquivo: `backend/src/main/java/com/br/rkfbank/clients/viacep/ClienteViaCep.java`

```java
ResponseViaCepDto resposta = restClient.get()
        .uri("/{cep}/json/", cep)
        .retrieve()
        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new ServicoExternoException("ViaCEP indisponível");
        })
        .body(ResponseViaCepDto.class);

if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
    throw new NaoProcessavelException("CEP não encontrado");
}
```

### 3.6 Handler global de excecao

Responsabilidade: padronizar resposta de erro HTTP.

Arquivo: `backend/src/main/java/com/br/rkfbank/controllers/handlers/ControllerExceptionHandler.java`

```java
@ExceptionHandler(NaoProcessavelException.class)
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public ErroResponse naoProcessavel(NaoProcessavelException ex, HttpServletRequest request) {
    return montar(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), List.of());
}
```

### 3.7 Validation customizada

Responsabilidade: validar algoritmo de CPF na borda da API.

- anotacao: `validation/CpfValido.java`
- validador: `validation/CpfValidoValidator.java`
- uso no request: `CadastroClienteRequestDto.cpf`

## 4) Estado funcional implementado

- `POST /api/clientes`
- `GET /api/clientes` (paginado e nao paginado)
- `POST /api/clientes/{clienteId}/contas`
- `GET /api/contas` (paginado e nao paginado)
- `POST /api/contas/{contaId}/deposito`
- `POST /api/contas/{contaId}/saque`
- `POST /api/transferencias`
- `GET /api/contas/{contaId}/lancamentos`

## 5) Cobertura e testes por camada

Classes atuais de teste:

- `backend/src/test/java/com/br/rkfbank/validation/CpfValidoValidatorTest.java`
- `backend/src/test/java/com/br/rkfbank/clients/viacep/ClienteViaCepTest.java`
- `backend/src/test/java/com/br/rkfbank/services/ClienteServiceTest.java`
- `backend/src/test/java/com/br/rkfbank/services/ContaServiceTest.java`
- `backend/src/test/java/com/br/rkfbank/controllers/ClienteControllerTest.java`
- `backend/src/test/java/com/br/rkfbank/controllers/ContaControllerTest.java`
- `backend/src/test/java/com/br/rkfbank/controllers/handlers/ControllerExceptionHandlerTest.java`
- `backend/src/test/java/com/br/rkfbank/entities/EntitiesAndEnumsTest.java`

Resultado atual de cobertura (JaCoCo / instrucoes): **100%**.

## 6) UUID no MySQL

- API exposta em UUID texto
- persistencia em `binary(16)`

Workbench:

```sql
SELECT BIN_TO_UUID(id) AS id FROM tb_cliente;
SELECT * FROM tb_conta WHERE id = UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000');
```

## 7) Subida local e coverage

```powershell
Set-Location "C:\Users\Estudo$\Documents\rkfbank"
docker compose up -d
.\mvnw.cmd clean test
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Relatorio JaCoCo:

- `target/site/jacoco/index.html`

## 8) Teste E2E

Fluxo completo com requests/responses reais:

- `backend/README_TESTES_E2E.md`
