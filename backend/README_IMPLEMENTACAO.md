# RKF Bank - Guia final de implementacao

Este e o documento unico do MVP. Leia os conceitos primeiro. Depois siga os steps na ordem. Cada step diz **o que fazer**, **onde colocar** e **como fazer**, com o codigo pronto para copiar.

## O que este MVP faz

- Cadastra cliente.
- Quando o cliente informa o CEP, o backend consulta o ViaCEP e preenche rua, bairro, cidade e UF. O cliente informa apenas numero e complemento.
- Relaciona cliente com conta.
- Faz deposito, saque e transferencia.
- Grava cada movimentacao em `Lancamento`.
- Usa `BigDecimal` para dinheiro.

## Conceitos que voce precisa guardar

### Camadas

- `controlador`: recebe HTTP e devolve HTTP. Nao calcula saldo.
- `servico`: regra de negocio. E aqui que o lancamento nasce.
- `repositorio`: fala com o PostgreSQL.
- `dominio`: tabelas (`Cliente`, `Endereco`, `Conta`, `Lancamento`).
- `dto`: o que entra (`Request`) e o que sai (`Response`).
- `clienteexterno`: chamada ao ViaCEP.
- `excecao`: erro sempre no mesmo formato.

Causa: se tudo ficar na mesma classe, qualquer mudanca quebra o resto. Efeito: voce sabe onde procurar o erro.

### Request e Response

- `Request` = JSON que o frontend envia.
- `Response` = JSON que o backend devolve.
- Entidade JPA nao sai na API. Ela e detalhe interno do banco.

### Relacao entre as classes

```text
Cliente (1) ---- (1) Endereco
Cliente (1) ---- (N) Conta
Conta   (1) ---- (N) Lancamento
```

- `Cliente` e a pessoa.
- `Endereco` e onde ela mora.
- `Conta` e o saldo de agora.
- `Lancamento` e o historico de como o saldo mudou.

`Lancamento` nao e preenchido pelo frontend. O `ServicoConta` cria um lancamento dentro de `depositar`, `sacar` e `transferir`. Sem isso existe saldo, mas nao existe extrato.

### Enum

`TipoConta` so aceita `CORRENTE` ou `POUPANCA`. `TipoLancamento` so aceita `DEPOSITO`, `SAQUE` ou `TRANSFERENCIA`.

Causa: string livre vira `"corrente"`, `"Corrente"` e `"coreente"` no mesmo banco. Efeito: a regra futura consegue fazer `if (conta.getTipoConta() == TipoConta.POUPANCA)` sem comparar texto.

### Composicao

Composicao aqui e um objeto dentro de outro. `ClienteResponse` contem `EnderecoResponse` no campo `endereco`. O frontend recebe pessoa e endereco na mesma resposta.

### BigDecimal

Dinheiro nao usa `double`. `double` arredonda errado. `BigDecimal` guarda decimal exato. No banco a coluna e `NUMERIC(19,2)`. No JSON o campo se chama `valor`, por exemplo `"50.00"`.

Operacoes:

- somar: `saldo.add(valor)`
- subtrair: `saldo.subtract(valor)`
- comparar: `saldo.compareTo(valor) < 0`

Nao use `+`, `-` ou `>`.

### ViaCEP, do CEP digitado ate a resposta

1. O usuario digita CEP, numero e complemento.
2. O frontend chama `POST /api/clientes`.
3. O controlador valida o `CadastroClienteRequest`.
4. O servico tira tudo que nao for numero do CEP.
5. O servico so chama o ViaCEP se o CEP tiver 8 digitos. CEP com 8 digitos que o ViaCEP nao encontra volta `422`.
6. O `ClienteViaCep` chama `GET https://viacep.com.br/ws/{cep}/json/`.
7. Se o CEP existe, voltam logradouro, bairro, cidade, UF e IBGE.
8. Se o CEP tem 8 digitos mas nao existe, o JSON vem com `"erro": true`.
9. O servico junta os dados do ViaCEP com numero e complemento do usuario.
10. Salva cliente e endereco juntos.
11. Devolve `201` com `ClienteResponse`.

## Mapa de pastas

Tudo fica em `backend/src/main/java/com/br/rkfbank`.

```text
config/ConfiguracaoCors.java
controlador/ControladorCliente.java
controlador/ControladorConta.java
servico/ServicoCliente.java
servico/ServicoConta.java
repositorio/RepositorioCliente.java
repositorio/RepositorioConta.java
repositorio/RepositorioLancamento.java
clienteexterno/viacep/ClienteViaCep.java
clienteexterno/viacep/RespostaViaCepDTO.java
dto/cliente/CadastroClienteRequest.java
dto/cliente/ClienteResponse.java
dto/cliente/EnderecoResponse.java
dto/conta/AbrirContaRequest.java
dto/conta/ContaResponse.java
dto/conta/DepositarRequest.java
dto/conta/SacarRequest.java
dto/conta/TransferirRequest.java
dto/conta/LancamentoResponse.java
dto/comum/ErroCampoResponse.java
dto/comum/ErroResponse.java
dominio/TipoConta.java
dominio/TipoLancamento.java
dominio/Endereco.java
dominio/Cliente.java
dominio/Conta.java
dominio/Lancamento.java
excecao/ExcecaoNegocio.java
excecao/ExcecaoClienteNaoEncontrado.java
excecao/ExcecaoContaNaoEncontrada.java
excecao/ExcecaoNaoProcessavel.java
excecao/ExcecaoServicoExterno.java
excecao/TratadorGlobalExcecao.java
```

Configuracao fica em `backend/src/main/resources/application.properties`. Banco fica na raiz: `.env` e `docker-compose.yml`.

## Implementacao

Siga a ordem. Cada passo depende do anterior.

### Step 1 - Subir o PostgreSQL

**O que fazer:** criar o banco local que a aplicacao vai usar.

**Onde:** raiz do projeto, ao lado do `pom.xml`.

**Como:** crie `.env` e `docker-compose.yml`. O Compose le o `.env` automaticamente.

`.env`

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=rkfbank
POSTGRES_PORT=5441
POSTGRES_VERSION=17-alpine
```

`docker-compose.yml`

```yaml
services:
  database:
    image: postgres:${POSTGRES_VERSION}
    container_name: rkfbank-postgres
    restart: unless-stopped
    ports:
      - "${POSTGRES_PORT}:5432"
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - pg-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

volumes:
  pg-data:
```

Por que cada campo existe:

- `image`: qual Postgres sobe.
- `ports`: `5441` na sua maquina aponta para `5432` dentro do container.
- `environment`: cria usuario, senha e banco na primeira subida.
- `volumes`: os dados continuam depois de desligar o container.
- `healthcheck`: mostra se o banco ja aceita conexao.

Adicione `.env` no `.gitignore`. Senha nao deve ir para o Git.

### Step 2 - Dizer ao Spring onde esta o banco e o ViaCEP

**O que fazer:** configurar conexao e URL externa.

**Onde:** `backend/src/main/resources/application.properties`

**Como:**

```properties
spring.application.name=rkfbank

spring.datasource.url=jdbc:postgresql://localhost:5441/rkfbank
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

viacep.base-url=https://viacep.com.br/ws
viacep.timeout-ms=3000
```

`ddl-auto=update` cria e ajusta tabela no estudo. Em producao isso sai e entra migracao versionada. `open-in-view=false` evita consulta preguicosa fora do servico.

O `pom.xml` esta na raiz e o codigo esta em `backend/src`. O Maven, por padrao, le `src/main/java`. Para este projeto subir pela raiz, o bloco `build` do `pom.xml` precisa apontar para `backend`:

```xml
<build>
  <sourceDirectory>backend/src/main/java</sourceDirectory>
  <testSourceDirectory>backend/src/test/java</testSourceDirectory>
  <resources>
    <resource>
      <directory>backend/src/main/resources</directory>
    </resource>
  </resources>
  <testResources>
    <testResource>
      <directory>backend/src/test/resources</directory>
    </testResource>
  </testResources>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

### Step 3 - Liberar o frontend local

**O que fazer:** permitir que o Next.js em `localhost:3000` chame a API.

**Onde:** `backend/src/main/java/com/br/rkfbank/config/ConfiguracaoCors.java`

**Como:**

```java
package com.br.rkfbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoCors {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }
}
```

Sem isso o navegador bloqueia a chamada mesmo com o backend funcionando.

### Step 4 - Criar os enums

**O que fazer:** limitar os tipos validos de conta e de movimentacao.

**Onde:**

- `backend/src/main/java/com/br/rkfbank/dominio/TipoConta.java`
- `backend/src/main/java/com/br/rkfbank/dominio/TipoLancamento.java`

**Como:**

```java
package com.br.rkfbank.dominio;

public enum TipoConta {
    CORRENTE,
    POUPANCA
}
```

```java
package com.br.rkfbank.dominio;

public enum TipoLancamento {
    DEPOSITO,
    SAQUE,
    TRANSFERENCIA
}
```

`CORRENTE` e conta do dia a dia. `POUPANCA` existe desde ja para a regra futura de rendimento, sem mudar o tipo da coluna. Nas entidades, `@Enumerated(EnumType.STRING)` grava o nome do enum, nao um numero. Se a ordem do enum mudar, o banco nao quebra.

### Step 5 - Criar as entidades

**O que fazer:** modelar as tabelas e as relacoes.

**Onde:** pacote `com.br.rkfbank.dominio`.

**Como:** copie as quatro classes abaixo.

`dominio/Endereco.java` guarda o endereco oficial mais o que o usuario completa.

```java
package com.br.rkfbank.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "enderecos")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 9)
    private String cep;

    @Column(nullable = false)
    private String logradouro;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private String ibge;

    @Column(nullable = false)
    private String numero;

    private String complemento;

    public UUID getId() { return id; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getIbge() { return ibge; }
    public void setIbge(String ibge) { this.ibge = ibge; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
}
```

`dominio/Cliente.java` e o dono do endereco e das contas. `cascade = ALL` salva o endereco junto com o cliente.

```java
package com.br.rkfbank.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clientes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cliente_cpf", columnNames = "cpf"),
    @UniqueConstraint(name = "uk_cliente_email", columnNames = "email")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conta> contas = new ArrayList<>();

    public UUID getId() { return id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public List<Conta> getContas() { return contas; }
}
```

`dominio/Conta.java` guarda o saldo atual. `@Version` evita que duas operacoes simultaneas gravem por cima uma da outra sem o JPA perceber.

```java
package com.br.rkfbank.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_conta_numero", columnNames = "numero_conta")
})
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_conta", nullable = false)
    private String numeroConta;

    @Column(nullable = false, length = 4)
    private String agencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipoContaEnum;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false)
    private boolean ativa;

    @Version
    private Long versao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public UUID getId() { return id; }
    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }
    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public TipoConta getTipoConta() { return tipoContaEnum; }
    public void setTipoConta(TipoConta tipoContaEnum) { this.tipoContaEnum = tipoContaEnum; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
```

`dominio/Lancamento.java` e o extrato. Deposito preenche so destino. Saque preenche so origem. Transferencia preenche os dois. Quem cria essa classe em tempo de execucao e o `ServicoConta`, nao o frontend.

```java
package com.br.rkfbank.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lancamentos")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipoLancamento;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    private UUID contaOrigemId;
    private UUID contaDestinoId;
    private String descricao;

    @Column(nullable = false)
    private Instant dataHora;

    @PrePersist
    void aoSalvar() {
        this.dataHora = Instant.now();
    }

    public UUID getId() { return id; }
    public TipoLancamento getTipoLancamento() { return tipoLancamento; }
    public void setTipoLancamento(TipoLancamento tipoLancamento) { this.tipoLancamento = tipoLancamento; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public UUID getContaOrigemId() { return contaOrigemId; }
    public void setContaOrigemId(UUID contaOrigemId) { this.contaOrigemId = contaOrigemId; }
    public UUID getContaDestinoId() { return contaDestinoId; }
    public void setContaDestinoId(UUID contaDestinoId) { this.contaDestinoId = contaDestinoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Instant getDataHora() { return dataHora; }
}
```

### Step 6 - Criar os repositorios

**O que fazer:** criar o acesso ao banco sem SQL manual.

**Onde:** pacote `com.br.rkfbank.repositorio`.

**Como:**

```java
package com.br.rkfbank.repositorio;

import com.br.rkfbank.dominio.Cliente;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCliente extends JpaRepository<Cliente, UUID> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
}
```

```java
package com.br.rkfbank.repositorio;

import com.br.rkfbank.dominio.Conta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioConta extends JpaRepository<Conta, UUID> {
    boolean existsByNumeroConta(String numeroConta);
}
```

```java
package com.br.rkfbank.repositorio;

import com.br.rkfbank.dominio.Lancamento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioLancamento extends JpaRepository<Lancamento, UUID> {
    List<Lancamento> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(UUID contaOrigemId, UUID contaDestinoId);
}
```

O ultimo metodo alimenta o extrato: busca lancamento em que a conta foi origem ou destino.

### Step 7 - Criar Request e Response

**O que fazer:** definir o contrato HTTP. Entrada termina com `Request`. Saida termina com `Response`.

**Onde:** pacotes `dto.cliente`, `dto.conta` e `dto.comum`.

**Como:**

`dto/cliente/CadastroClienteRequest.java`

```java
package com.br.rkfbank.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
    @NotBlank(message = "nomeCompleto e obrigatorio")
    @Size(max = 120, message = "nomeCompleto deve ter no maximo 120 caracteres")
    String nomeCompleto,
    @NotBlank(message = "cpf e obrigatorio")
    @Pattern(regexp = "\\d{11}", message = "cpf deve ter 11 digitos numericos")
    String cpf,
    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    String email,
    @NotBlank(message = "telefone e obrigatorio")
    @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 digitos")
    String telefone,
    @NotBlank(message = "cep e obrigatorio")
    @Pattern(regexp = "\\d{8}", message = "cep deve ter 8 digitos numericos")
    String cep,
    @NotBlank(message = "numero e obrigatorio")
    String numero,
    @Size(max = 80, message = "complemento deve ter no maximo 80 caracteres")
    String complemento
) {}
```

`dto/cliente/EnderecoResponse.java`

```java
package com.br.rkfbank.dto.cliente;

public record EnderecoResponse(
    String cep,
    String logradouro,
    String bairro,
    String cidade,
    String uf,
    String ibge,
    String numero,
    String complemento
) {}
```

`dto/cliente/ClienteResponse.java` contem `EnderecoResponse`. Esse e o ponto de composicao.

```java
package com.br.rkfbank.dto.cliente;

import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String nomeCompleto,
    String cpf,
    String email,
    String telefone,
    EnderecoResponse endereco
) {}
```

`dto/conta/AbrirContaRequest.java`

```java
package com.br.rkfbank.dto.conta;

import com.br.rkfbank.dominio.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AbrirContaRequest(
    @NotBlank(message = "agencia e obrigatoria")
    @Pattern(regexp = "\\d{4}", message = "agencia deve ter 4 digitos")
    String agencia,

    @NotBlank(message = "numeroConta e obrigatorio")
    @Pattern(regexp = "\\d{5,12}", message = "numeroConta deve ter de 5 a 12 digitos")
    String numeroConta,

    @NotNull(message = "tipoContaEnum e obrigatorio e deve ser CORRENTE ou POUPANCA")
    TipoConta tipoContaEnum
) {}
```

`dto/conta/ContaResponse.java`

```java
package com.br.rkfbank.dto.conta;

import com.br.rkfbank.dominio.TipoConta;
import java.math.BigDecimal;
import java.util.UUID;

public record ContaResponse(
    UUID id,
    UUID clienteId,
    String agencia,
    String numeroConta,
    TipoConta tipoContaEnum,
    BigDecimal saldo,
    boolean ativa
) {}
```

`dto/conta/DepositarRequest.java`

```java
package com.br.rkfbank.dto.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DepositarRequest(
    @NotNull(message = "valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no maximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no maximo 140 caracteres")
    String descricao
) {}
```

`dto/conta/SacarRequest.java` e igual, so muda o nome do record para `SacarRequest`.

`dto/conta/TransferirRequest.java`

```java
package com.br.rkfbank.dto.conta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferirRequest(
    @NotNull(message = "contaOrigemId e obrigatoria")
    UUID contaOrigemId,

    @NotNull(message = "contaDestinoId e obrigatoria")
    UUID contaDestinoId,

    @NotNull(message = "valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
    @Digits(integer = 17, fraction = 2, message = "valor deve ter no maximo 2 casas decimais")
    BigDecimal valor,

    @Size(max = 140, message = "descricao deve ter no maximo 140 caracteres")
    String descricao
) {}
```

`dto/conta/LancamentoResponse.java`

```java
package com.br.rkfbank.dto.conta;

import com.br.rkfbank.dominio.TipoLancamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LancamentoResponse(
    UUID id,
    TipoLancamento tipoLancamento,
    BigDecimal valor,
    UUID contaOrigemId,
    UUID contaDestinoId,
    String descricao,
    Instant dataHora
) {}
```

`dto/comum/ErroCampoResponse.java`

```java
package com.br.rkfbank.dto.comum;

import java.time.Instant;
import java.util.List;

public record ErroCampoResponse(String campo, String mensagem) {}
`

dto/comum/ErroResponse.java

`java
package com.br.rkfbank.dto.comum;

import java.time.Instant;
import java.util.List;

public record ErroResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ErroCampoResponse> campos
) {}
```

### Step 8 - Tratar 400, 404 e 422 no handler

**O que fazer:** separar erro de formato, erro de recurso inexistente e erro de regra. Cada campo invalido do Request precisa voltar no JSON.

**Onde:** pacote `com.br.rkfbank.excecao`.

**Como o status e escolhido:**

| Situacao | Status | Excecao |
| --- | --- | --- |
| Campo ausente, formato errado, JSON ilegivel, UUID ou enum invalido na URL | `400 Bad Request` | `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` |
| Cliente nao encontrado em `ServicoCliente.buscar` | `404 Not Found` | `ExcecaoClienteNaoEncontrado` |
| Conta nao encontrada em deposito, saque, transferencia ou extrato | `404 Not Found` | `ExcecaoContaNaoEncontrada` |
| CEP de 8 digitos inexistente, saldo insuficiente, conta inativa ou transferencia para a mesma conta | `422 Unprocessable Entity` | `ExcecaoNaoProcessavel` |
| CPF, email ou numero de conta duplicado | `409 Conflict` | `ExcecaoNegocio` |
| ViaCEP fora do ar | `503 Service Unavailable` | `ExcecaoServicoExterno` |

`400` acontece antes da regra. O Bean Validation le a `message` de cada campo do Request. `404` so acontece depois que o formato esta certo e o id nao existe no banco. `422` significa que o JSON e valido, mas a operacao nao pode ser concluida.

**Como:** crie estas classes.

`excecao/ExcecaoNegocio.java`

```java
package com.br.rkfbank.excecao;

public class ExcecaoNegocio extends RuntimeException {
    public ExcecaoNegocio(String mensagem) { super(mensagem); }
}
```

`excecao/ExcecaoClienteNaoEncontrado.java`

```java
package com.br.rkfbank.excecao;

public class ExcecaoClienteNaoEncontrado extends RuntimeException {
    public ExcecaoClienteNaoEncontrado() {
        super("Cliente nao encontrado");
    }
}
```

`excecao/ExcecaoContaNaoEncontrada.java`

```java
package com.br.rkfbank.excecao;

public class ExcecaoContaNaoEncontrada extends RuntimeException {
    public ExcecaoContaNaoEncontrada() {
        super("Conta nao encontrada");
    }
}
```

`excecao/ExcecaoNaoProcessavel.java` cobre CEP inexistente, saldo e conta inativa. Esses casos sao `422`, nao `400`, porque o campo chegou bem formatado.

```java
package com.br.rkfbank.excecao;

public class ExcecaoNaoProcessavel extends RuntimeException {
    public ExcecaoNaoProcessavel(String mensagem) { super(mensagem); }
}
```

`excecao/ExcecaoServicoExterno.java`

```java
package com.br.rkfbank.excecao;

public class ExcecaoServicoExterno extends RuntimeException {
    public ExcecaoServicoExterno(String mensagem) { super(mensagem); }
}
```

`excecao/TratadorGlobalExcecao.java` e o unico lugar que transforma excecao em HTTP. O controlador nao faz `try/catch`.

```java
package com.br.rkfbank.excecao;

import com.br.rkfbank.dto.comum.ErroCampoResponse;
import com.br.rkfbank.dto.comum.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorGlobalExcecao {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse campoInvalido(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
            .toList();
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", request.getRequestURI(), campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return montar(
            HttpStatus.BAD_REQUEST,
            "JSON invalido ou tipo de campo incompativel",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse caminhoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String campo = ex.getName() == null ? "parametro" : ex.getName();
        return montar(
            HttpStatus.BAD_REQUEST,
            "Parametro de URL invalido",
            request.getRequestURI(),
            List.of(new ErroCampoResponse(campo, campo + " deve ser um UUID"))
        );
    }

    @ExceptionHandler(ExcecaoClienteNaoEncontrado.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse clienteNaoEncontrado(ExcecaoClienteNaoEncontrado ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ExcecaoContaNaoEncontrada.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse contaNaoEncontrada(ExcecaoContaNaoEncontrada ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ExcecaoNaoProcessavel.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse naoProcessavel(ExcecaoNaoProcessavel ex, HttpServletRequest request) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ExcecaoNegocio.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse conflito(ExcecaoNegocio ex, HttpServletRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ExcecaoServicoExterno.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse externo(ExcecaoServicoExterno ex, HttpServletRequest request) {
        return montar(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(), List.of());
    }

    private ErroResponse montar(HttpStatus status, String mensagem, String path, List<ErroCampoResponse> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }
}
```

Exemplo de `400` quando faltam `cpf` e `cep` ao mesmo tempo:

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/api/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "cpf e obrigatorio" },
    { "campo": "cep", "mensagem": "cep deve ter 8 digitos numericos" }
  ]
}
```

Exemplo de `404` ao depositar em conta que nao existe:

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conta nao encontrada",
  "path": "/api/contas/0f77d728-e8f8-4449-b8ac-2cb95a66c3a6/deposito",
  "campos": []
}
```

Exemplo de `422` com saldo insuficiente:

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente",
  "path": "/api/contas/0f77d728-e8f8-4449-b8ac-2cb95a66c3a6/saque",
  "campos": []
}
```

Quem lanca cada uma:

- `ServicoCliente.buscar` lanca `ExcecaoClienteNaoEncontrado` quando o id da URL nao existe.
- `ServicoConta.buscarAtiva` lanca `ExcecaoContaNaoEncontrada` antes de mexer no saldo.
- `ClienteViaCep` lanca `ExcecaoNaoProcessavel` quando o ViaCEP responde `"erro": true`.
- `ServicoConta` lanca `ExcecaoNaoProcessavel` para saldo, conta inativa e mesma conta.
- Duplicidade continua em `ExcecaoNegocio`, porque o recurso ja existe. Isso e conflito, nao campo invalido.

### Step 9 - Integrar o ViaCEP

**O que fazer:** isolar a chamada HTTP.

**Onde:** `backend/src/main/java/com/br/rkfbank/clienteexterno/viacep/`

**Como:**

`RespostaViaCepDTO.java`

```java
package com.br.rkfbank.clienteexterno.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RespostaViaCepDTO(
    String cep,
    String logradouro,
    String bairro,
    String localidade,
    String uf,
    String ibge,
    Boolean erro
) {}
```

`ClienteViaCep.java`

```java
package com.br.rkfbank.clienteexterno.viacep;

import com.br.rkfbank.excecao.ExcecaoNaoProcessavel;
import com.br.rkfbank.excecao.ExcecaoServicoExterno;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClienteViaCep {

    private final RestClient restClient;

    public ClienteViaCep(
        @Value("${viacep.base-url}") String baseUrl,
        @Value("${viacep.timeout-ms}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public RespostaViaCepDTO buscarPorCep(String cep) {
        try {
            RespostaViaCepDTO resposta = restClient.get()
                .uri("/{cep}/json/", cep)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new ExcecaoServicoExterno("ViaCEP indisponivel");
                })
                .body(RespostaViaCepDTO.class);

            if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
                throw new ExcecaoNaoProcessavel("CEP nao encontrado");
            }
            return resposta;
        } catch (ExcecaoNaoProcessavel | ExcecaoServicoExterno ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ExcecaoServicoExterno("Falha ao consultar ViaCEP");
        }
    }
}
```

O servico nao sabe URL, timeout nem status HTTP. Ele so pede `buscarPorCep`.

### Step 10 - Cadastrar cliente

**O que fazer:** validar, consultar CEP, montar endereco e devolver response.

**Onde:**

- `servico/ServicoCliente.java`
- `controlador/ControladorCliente.java`

**Como:**

```java
package com.br.rkfbank.servico;

import com.br.rkfbank.clienteexterno.viacep.ClienteViaCep;
import com.br.rkfbank.clienteexterno.viacep.RespostaViaCepDTO;
import com.br.rkfbank.dominio.Cliente;
import com.br.rkfbank.dominio.Endereco;
import com.br.rkfbank.dto.cliente.CadastroClienteRequest;
import com.br.rkfbank.dto.cliente.ClienteResponse;
import com.br.rkfbank.dto.cliente.EnderecoResponse;
import com.br.rkfbank.excecao.ExcecaoClienteNaoEncontrado;
import com.br.rkfbank.excecao.ExcecaoNegocio;
import com.br.rkfbank.repositorio.RepositorioCliente;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoCliente {

    private final RepositorioCliente repositorioCliente;
    private final ClienteViaCep clienteViaCep;

    public ServicoCliente(RepositorioCliente repositorioCliente, ClienteViaCep clienteViaCep) {
        this.repositorioCliente = repositorioCliente;
        this.clienteViaCep = clienteViaCep;
    }

    @Transactional
    public ClienteResponse cadastrar(CadastroClienteRequest request) {
        if (repositorioCliente.existsByCpf(request.cpf())) {
            throw new ExcecaoNegocio("CPF ja cadastrado");
        }
        if (repositorioCliente.existsByEmail(request.email())) {
            throw new ExcecaoNegocio("Email ja cadastrado");
        }

        String cep = request.cep().replaceAll("\\D", "");
        if (!cep.matches("\\d{8}")) {
            throw new ExcecaoNegocio("CEP invalido");
        }

        RespostaViaCepDTO viaCep = clienteViaCep.buscarPorCep(cep);

        Endereco endereco = new Endereco();
        endereco.setCep(viaCep.cep());
        endereco.setLogradouro(viaCep.logradouro());
        endereco.setBairro(viaCep.bairro());
        endereco.setCidade(viaCep.localidade());
        endereco.setUf(viaCep.uf());
        endereco.setIbge(viaCep.ibge());
        endereco.setNumero(request.numero());
        endereco.setComplemento(request.complemento());

        Cliente cliente = new Cliente();
        cliente.setNomeCompleto(request.nomeCompleto());
        cliente.setCpf(request.cpf());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(endereco);

        return paraResponse(repositorioCliente.save(cliente));
    }

    public Cliente buscar(UUID id) {
        return repositorioCliente.findById(id)
            .orElseThrow(ExcecaoClienteNaoEncontrado::new);
    }

    private ClienteResponse paraResponse(Cliente cliente) {
        Endereco endereco = cliente.getEndereco();
        return new ClienteResponse(
            cliente.getId(),
            cliente.getNomeCompleto(),
            cliente.getCpf(),
            cliente.getEmail(),
            cliente.getTelefone(),
            new EnderecoResponse(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getIbge(),
                endereco.getNumero(),
                endereco.getComplemento()
            )
        );
    }
}
```

```java
package com.br.rkfbank.controlador;

import com.br.rkfbank.dto.cliente.CadastroClienteRequest;
import com.br.rkfbank.dto.cliente.ClienteResponse;
import com.br.rkfbank.servico.ServicoCliente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ControladorCliente {

    private final ServicoCliente servicoCliente;

    public ControladorCliente(ServicoCliente servicoCliente) {
        this.servicoCliente = servicoCliente;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse cadastrar(@Valid @RequestBody CadastroClienteRequest request) {
        return servicoCliente.cadastrar(request);
    }
}
```

`@Valid` dispara as anotacoes do Request antes do servico. `@Transactional` garante que cliente e endereco entram juntos ou nao entram.

### Step 11 - Abrir conta, movimentar e gravar lancamento

**O que fazer:** criar a conta do cliente e as tres operacoes. Cada operacao muda `Conta` e cria `Lancamento`.

**Onde:**

- `servico/ServicoConta.java`
- `controlador/ControladorConta.java`

**Como:**

```java
package com.br.rkfbank.servico;

import com.br.rkfbank.dominio.Cliente;
import com.br.rkfbank.dominio.Conta;
import com.br.rkfbank.dominio.Lancamento;
import com.br.rkfbank.dominio.TipoLancamento;
import com.br.rkfbank.dto.conta.AbrirContaRequest;
import com.br.rkfbank.dto.conta.ContaResponse;
import com.br.rkfbank.dto.conta.LancamentoResponse;
import com.br.rkfbank.excecao.ExcecaoContaNaoEncontrada;
import com.br.rkfbank.excecao.ExcecaoNaoProcessavel;
import com.br.rkfbank.excecao.ExcecaoNegocio;
import com.br.rkfbank.repositorio.RepositorioConta;
import com.br.rkfbank.repositorio.RepositorioLancamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoConta {

    private final RepositorioConta repositorioConta;
    private final RepositorioLancamento repositorioLancamento;
    private final ServicoCliente servicoCliente;

    public ServicoConta(
        RepositorioConta repositorioConta,
        RepositorioLancamento repositorioLancamento,
        ServicoCliente servicoCliente
    ) {
        this.repositorioConta = repositorioConta;
        this.repositorioLancamento = repositorioLancamento;
        this.servicoCliente = servicoCliente;
    }

    @Transactional
    public ContaResponse abrir(UUID clienteId, AbrirContaRequest request) {
        Cliente cliente = servicoCliente.buscar(clienteId);
        if (repositorioConta.existsByNumeroConta(request.numeroConta())) {
            throw new ExcecaoNegocio("Numero de conta ja existe");
        }

        Conta conta = new Conta();
        conta.setCliente(cliente);
        conta.setAgencia(request.agencia());
        conta.setNumeroConta(request.numeroConta());
        conta.setTipoConta(request.tipoContaEnum());
        conta.setSaldo(new BigDecimal("0.00"));
        conta.setAtiva(true);
        return paraResponse(repositorioConta.save(conta));
    }

    @Transactional
    public void depositar(UUID contaId, BigDecimal valor, String descricao) {
        Conta conta = buscarAtiva(contaId);
        BigDecimal valorValido = validarValor(valor);
        conta.setSaldo(conta.getSaldo().add(valorValido));
        repositorioConta.save(conta);
        salvarLancamento(TipoLancamento.DEPOSITO, valorValido, null, conta.getId(), descricao);
    }

    @Transactional
    public void sacar(UUID contaId, BigDecimal valor, String descricao) {
        Conta conta = buscarAtiva(contaId);
        BigDecimal valorValido = validarValor(valor);
        if (conta.getSaldo().compareTo(valorValido) < 0) {
            throw new ExcecaoNaoProcessavel("Saldo insuficiente");
        }
        conta.setSaldo(conta.getSaldo().subtract(valorValido));
        repositorioConta.save(conta);
        salvarLancamento(TipoLancamento.SAQUE, valorValido, conta.getId(), null, descricao);
    }

    @Transactional
    public void transferir(UUID origemId, UUID destinoId, BigDecimal valor, String descricao) {
        if (origemId.equals(destinoId)) {
            throw new ExcecaoNaoProcessavel("Conta de origem e destino nao podem ser iguais");
        }
        BigDecimal valorValido = validarValor(valor);
        Conta origem = buscarAtiva(origemId);
        Conta destino = buscarAtiva(destinoId);
        if (origem.getSaldo().compareTo(valorValido) < 0) {
            throw new ExcecaoNaoProcessavel("Saldo insuficiente para transferencia");
        }
        origem.setSaldo(origem.getSaldo().subtract(valorValido));
        destino.setSaldo(destino.getSaldo().add(valorValido));
        repositorioConta.save(origem);
        repositorioConta.save(destino);
        salvarLancamento(TipoLancamento.TRANSFERENCIA, valorValido, origem.getId(), destino.getId(), descricao);
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listarLancamentos(UUID contaId) {
        buscarAtiva(contaId);
        return repositorioLancamento
            .findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId)
            .stream()
            .map(this::paraResponse)
            .toList();
    }

    private Conta buscarAtiva(UUID contaId) {
        Conta conta = repositorioConta.findById(contaId)
            .orElseThrow(ExcecaoContaNaoEncontrada::new);
        if (!conta.isAtiva()) {
            throw new ExcecaoNaoProcessavel("Conta inativa");
        }
        return conta;
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcecaoNaoProcessavel("Valor deve ser maior que zero");
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private void salvarLancamento(
        TipoLancamento tipo,
        BigDecimal valor,
        UUID origem,
        UUID destino,
        String descricao
    ) {
        Lancamento lancamento = new Lancamento();
        lancamento.setTipoLancamento(tipo);
        lancamento.setValor(valor);
        lancamento.setContaOrigemId(origem);
        lancamento.setContaDestinoId(destino);
        lancamento.setDescricao(descricao);
        repositorioLancamento.save(lancamento);
    }

    private ContaResponse paraResponse(Conta conta) {
        return new ContaResponse(
            conta.getId(),
            conta.getCliente().getId(),
            conta.getAgencia(),
            conta.getNumeroConta(),
            conta.getTipoConta(),
            conta.getSaldo(),
            conta.isAtiva()
        );
    }

    private LancamentoResponse paraResponse(Lancamento lancamento) {
        return new LancamentoResponse(
            lancamento.getId(),
            lancamento.getTipoLancamento(),
            lancamento.getValor(),
            lancamento.getContaOrigemId(),
            lancamento.getContaDestinoId(),
            lancamento.getDescricao(),
            lancamento.getDataHora()
        );
    }
}
```

```java
package com.br.rkfbank.controlador;

import com.br.rkfbank.dto.conta.AbrirContaRequest;
import com.br.rkfbank.dto.conta.ContaResponse;
import com.br.rkfbank.dto.conta.DepositarRequest;
import com.br.rkfbank.dto.conta.LancamentoResponse;
import com.br.rkfbank.dto.conta.SacarRequest;
import com.br.rkfbank.dto.conta.TransferirRequest;
import com.br.rkfbank.servico.ServicoConta;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ControladorConta {

    private final ServicoConta servicoConta;

    public ControladorConta(ServicoConta servicoConta) {
        this.servicoConta = servicoConta;
    }

    @PostMapping("/clientes/{clienteId}/contas")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse abrir(@PathVariable UUID clienteId, @Valid @RequestBody AbrirContaRequest request) {
        return servicoConta.abrir(clienteId, request);
    }

    @PostMapping("/contas/{contaId}/deposito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void depositar(@PathVariable UUID contaId, @Valid @RequestBody DepositarRequest request) {
        servicoConta.depositar(contaId, request.valor(), request.descricao());
    }

    @PostMapping("/contas/{contaId}/saque")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sacar(@PathVariable UUID contaId, @Valid @RequestBody SacarRequest request) {
        servicoConta.sacar(contaId, request.valor(), request.descricao());
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferir(@Valid @RequestBody TransferirRequest request) {
        servicoConta.transferir(
            request.contaOrigemId(),
            request.contaDestinoId(),
            request.valor(),
            request.descricao()
        );
    }

    @GetMapping("/contas/{contaId}/lancamentos")
    public List<LancamentoResponse> listar(@PathVariable UUID contaId) {
        return servicoConta.listarLancamentos(contaId);
    }
}
```

`@Transactional` na transferencia e obrigatorio. Se o credito falhar depois do debito, o banco desfaz os dois. Por isso transferencia nao deve ser duas chamadas HTTP separadas.

## Contrato para o frontend

Base: `http://localhost:8080/api`

- `POST /clientes` cria cliente e devolve endereco completo. Status `201`.
- `POST /clientes/{clienteId}/contas` abre conta com saldo `0.00`. Status `201`.
- `POST /contas/{contaId}/deposito` soma saldo e grava `DEPOSITO`. Status `204`.
- `POST /contas/{contaId}/saque` subtrai saldo e grava `SAQUE`. Status `204`.
- `POST /transferencias` debita, credita e grava `TRANSFERENCIA`. Status `204`.
- `GET /contas/{contaId}/lancamentos` devolve o historico. Status `200`.

Dinheiro sempre no campo `valor`, com duas casas: `"50.00"`.

Cadastro:

```json
{
  "nomeCompleto": "Joao Silva",
  "cpf": "12345678901",
  "email": "joao@email.com",
  "telefone": "11999999999",
  "cep": "01001000",
  "numero": "123",
  "complemento": "Apto 45"
}
```

A resposta traz `endereco` dentro do cliente. Esse objeto interno e a composicao.

Erro sempre neste formato:

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente",
  "path": "/api/contas/123/saque",
  "campos": []
}
```

## Como subir o projeto

Na raiz `C:\Users\Estudo$\Documents\rkfbank`, use dois terminais.

Terminal 1, banco:

```powershell
docker compose up -d
docker compose ps
```

Espere o status `healthy`.

Terminal 2, API:

```powershell
.\mvnw.cmd spring-boot:run
```

A API fica em `http://localhost:8080`. Para parar, use `Ctrl+C` na API e depois:

```powershell
docker compose down
```

`docker compose down -v` apaga os dados do banco. Use so quando quiser comecar do zero.

## Como testar manualmente

Use outro PowerShell com a API no ar. O CEP `01001000` existe no ViaCEP.

```powershell
$cliente = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/clientes -ContentType "application/json" -Body '{"nomeCompleto":"Joao Silva","cpf":"12345678901","email":"joao@email.com","telefone":"11999999999","cep":"01001000","numero":"123","complemento":"Apto 45"}'
$cliente | ConvertTo-Json -Depth 5
```

Confira se `endereco.logradouro` voltou preenchido. Esse e o teste da integracao.

```powershell
$conta = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/clientes/$($cliente.id)/contas" -ContentType "application/json" -Body '{"agencia":"0001","numeroConta":"123456","tipoContaEnum":"CORRENTE"}'
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/contas/$($conta.id)/deposito" -ContentType "application/json" -Body '{"valor":"50.00","descricao":"Deposito inicial"}'
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/contas/$($conta.id)/saque" -ContentType "application/json" -Body '{"valor":"15.00","descricao":"Saque"}'
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/contas/$($conta.id)/lancamentos"
```

O extrato deve mostrar um `DEPOSITO` de `50.00` e um `SAQUE` de `15.00`.

Para transferencia, cadastre outro cliente com outro CPF e email, abra outra conta, deposite na primeira e envie:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/transferencias -ContentType "application/json" -Body (@{ contaOrigemId = $conta.id; contaDestinoId = $outraConta.id; valor = "10.00"; descricao = "Teste" } | ConvertTo-Json)
```

Teste tambem o erro. Saque `"9999.00"` em conta sem saldo. A resposta deve ser `422` com `message` igual a `Saldo insuficiente`. Cliente ou conta inexistente volta `404`. Campo invalido volta `400` com `campos`. CEP `99999999` no cadastro deve voltar `422`.

## Como testar automaticamente

**Onde:** `backend/src/test/java/com/br/rkfbank/servico/ServicoContaTest.java`

**Como:** teste a regra sem banco e sem ViaCEP. O mock substitui o repositorio.

```java
package com.br.rkfbank.servico;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dominio.Conta;
import com.br.rkfbank.excecao.ExcecaoNaoProcessavel;
import com.br.rkfbank.repositorio.RepositorioConta;
import com.br.rkfbank.repositorio.RepositorioLancamento;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicoContaTest {

    @Mock
    private RepositorioConta repositorioConta;

    @Mock
    private RepositorioLancamento repositorioLancamento;

    @Mock
    private ServicoCliente servicoCliente;

    @InjectMocks
    private ServicoConta servicoConta;

    @Test
    void deveRecusarSaqueSemSaldo() {
        UUID id = UUID.randomUUID();
        Conta conta = new Conta();
        conta.setSaldo(new BigDecimal("10.00"));
        conta.setAtiva(true);
        when(repositorioConta.findById(id)).thenReturn(Optional.of(conta));

        assertThrows(ExcecaoNaoProcessavel.class, () -> servicoConta.sacar(id, new BigDecimal("15.00"), "teste"));
    }
}
```

Rode na raiz:

```powershell
.\mvnw.cmd test
```

O teste passa se a conta de `10.00` recusar o saque de `15.00`. Esse e o primeiro teste. Depois repita a mesma ideia para deposito somar saldo, transferencia exigir contas diferentes e cadastro recusar CPF duplicado.

