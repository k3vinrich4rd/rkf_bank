# RKF Bank — Backend

API REST do RKF Bank: clientes, contas, movimentações e extrato.

| | |
|---|---|
| **Responsável** | Kevin ([@k3vinrich4rd](https://github.com/k3vinrich4rd)) |
| **Stack** | Java 25 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA (Hibernate) · Jakarta Validation · RestClient |
| **Banco** | MySQL 8.4 (Docker) — H2 em memória disponível para testes |
| **Build** | Maven Wrapper (`pom.xml` na raiz do repositório) · JaCoCo para cobertura |
| **Porta** | `8080` |

## 📄 Documentos do backend

| Documento | Conteúdo |
|---|---|
| [README_CONTRATO_API.md](README_CONTRATO_API.md) | **fonte da verdade** de rotas, payloads, regras e erros |
| [README_IMPLEMENTACAO.md](README_IMPLEMENTACAO.md) | arquitetura, camadas, decisões internas e **como criar um endpoint** |
| [README_TESTES_E2E.md](README_TESTES_E2E.md) | roteiro de teste manual do fluxo completo (Postman/Insomnia/curl) |

---

## ▶️ Como rodar

> Todos os comandos rodam na **raiz do repositório** (onde está o `pom.xml`), não dentro de `backend/`.

```bash
# 1. banco
cp .env.example .env
docker compose up -d

# 2. variáveis no terminal (o Spring não lê o .env sozinho)
set -a && source .env && set +a           # Git Bash / Linux / macOS

# 3. API
./mvnw spring-boot:run                    # Windows: .\mvnw.cmd spring-boot:run
```

No PowerShell, o passo 2 é:

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $nome, $valor = $_ -split '=', 2; Set-Item "env:$nome" $valor
}
```

Conferindo: `curl http://localhost:8080/clientes` → `[]`.

### Variáveis de ambiente

| Variável | Padrão no `.env.example` | Usada por |
|---|---|---|
| `MYSQL_USER` | `usuario-do-banco` | Docker e Spring |
| `MYSQL_PASSWORD` | `senha-do-banco` | Docker e Spring |
| `MYSQL_DATABASE` | `rkfbank` | Docker e Spring |
| `MYSQL_PORT` | `5441` | Docker (porta exposta) e Spring |
| `MYSQL_ROOT_PASSWORD` | `senha-root-do-banco` | só Docker |
| `MYSQL_VERSION` | `8.4` | só Docker |

> ⚠️ Se as variáveis **não** estiverem no terminal, o Spring usa os padrões de
> `application.properties` (`rkfbank` / `rkfbank`), que **não batem** com o usuário criado pelo Docker —
> resultado: `Access denied for user`. Ver [débitos técnicos](../planing/debitos-tecnicos.md).

### Configurações da aplicação

Arquivo: [`src/main/resources/application.properties`](src/main/resources/application.properties)

| Propriedade | Valor | Observação |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `update` | o Hibernate cria/altera tabelas sozinho (trocar por Flyway — débito técnico) |
| `spring.jpa.show-sql` | `true` | mostra o SQL no console; útil para estudar, ruim em produção |
| `viacep.base-url` | `https://viacep.com.br/ws` | |
| `viacep.timeout-ms` | `3000` | tempo máximo de conexão e de leitura do ViaCEP |

---

## 🧪 Testes

```bash
./mvnw test             # roda os testes
./mvnw clean verify     # testes + relatório de cobertura
```

Relatório de cobertura: abra `target/site/jacoco/index.html` no navegador.

| Classe de teste | O que cobre | Estilo |
|---|---|---|
| `CpfValidoValidatorTest` | algoritmo de CPF | unitário puro |
| `ClienteViaCepTest` | integração com ViaCEP (sucesso, CEP inexistente, falha) | unitário com mock |
| `ClienteServiceTest` / `ContaServiceTest` | regras de negócio | Mockito (`@ExtendWith(MockitoExtension.class)`) |
| `ClienteControllerTest` / `ContaControllerTest` | status HTTP e delegação ao service | Mockito |
| `ControllerExceptionHandlerTest` | exceção → status + corpo de erro | unitário |
| `EntitiesAndEnumsTest` | getters/setters, enums, `@PrePersist` | unitário |
| `RkfbankApplicationTests` | o contexto Spring sobe | `@SpringBootTest` |

> ⚠️ `RkfbankApplicationTests` sobe o Spring inteiro com o perfil padrão, então **precisa do MySQL no ar
> e das variáveis de ambiente carregadas** — senão `./mvnw test` falha nesse teste. Os demais rodam sem banco.

**Padrão de nome:** `deve<Resultado>[Quando<Condição>]` — ex.: `deveRetornarEnderecoQuandoViaCepResponderComSucesso`,
`deveRejeitarPaginacaoComPaginaNegativa`.
Use o padrão *Arrange / Act / Assert* (prepara, executa, verifica).

---

## 🗄️ Banco de dados

```mermaid
erDiagram
    tb_cliente ||--|| tb_endereco : "endereco_id"
    tb_cliente ||--o{ tb_conta : "cliente_id"
    tb_conta ||..o{ tb_lancamento : "conta_origem_id / conta_destino_id (sem FK)"

    tb_cliente {
        binary16 id PK
        string nome_completo
        string cpf UK "11 dígitos, sem máscara"
        string email UK
        string telefone "só dígitos"
        binary16 endereco_id FK
    }
    tb_endereco {
        binary16 id PK
        string cep "com hífen"
        string logradouro
        string bairro
        string cidade
        string uf
        string ibge
        string numero
        string complemento "opcional"
    }
    tb_conta {
        binary16 id PK
        string numero_conta UK
        string agencia
        string tipo_conta_enum "CORRENTE | POUPANCA"
        decimal saldo "19,2"
        boolean ativa
        bigint versao "lock otimista"
        binary16 cliente_id FK
    }
    tb_lancamento {
        binary16 id PK
        string tipo_lancamento_enum
        decimal valor "19,2"
        binary16 conta_origem_id "null no depósito"
        binary16 conta_destino_id "null no saque"
        string descricao
        timestamp data_hora
    }
```

Os IDs são UUID na API, mas ficam como `binary(16)` no MySQL. Para ler no Workbench/DBeaver:

```sql
SELECT BIN_TO_UUID(id) AS id, nome_completo, cpf, email FROM tb_cliente;

SELECT BIN_TO_UUID(id) AS id, numero_conta, saldo, ativa FROM tb_conta;

SELECT BIN_TO_UUID(id) AS id, tipo_lancamento_enum, valor,
       BIN_TO_UUID(conta_origem_id)  AS origem,
       BIN_TO_UUID(conta_destino_id) AS destino,
       data_hora
FROM tb_lancamento
ORDER BY data_hora DESC;

-- buscar por um UUID específico
SELECT * FROM tb_conta WHERE id = UUID_TO_BIN('407d727e-4e43-4c93-b01c-331cbaba42fb');
```

Conexão: `localhost:5441`, usuário e senha do seu `.env`.

---

## 🧯 Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Access denied for user 'rkfbank'` | variáveis do `.env` não carregadas no terminal | carregue o `.env` (seção "Como rodar") |
| `Communications link failure` | MySQL não está no ar ou ainda subindo | `docker compose ps` — espere `healthy` |
| `Rule 0 ... RequireJavaVersion failed` | JDK diferente de 25 | instale o JDK 25 e ajuste `JAVA_HOME` |
| `Port 8080 was already in use` | outra instância rodando | feche a outra ou mate o processo da porta |
| Cadastro devolve `503` | ViaCEP fora do ar ou sem internet | tente de novo; confira `https://viacep.com.br/ws/01001000/json/` |
| Cadastro devolve `422 CEP não encontrado` | CEP não existe no ViaCEP | use um CEP real, ex.: `01001000` |
| Mudei uma entidade e a coluna antiga ficou | `ddl-auto=update` não remove colunas | em dev: `docker compose down -v` apaga o banco e recria |
| `mvnw: Permission denied` (Linux/macOS) | arquivo sem permissão de execução | `chmod +x mvnw` |
