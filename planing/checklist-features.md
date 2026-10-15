# Checklist de features — rumo ao Pleno

Features do backend em **ordem de prioridade**. Para cada uma o ciclo é o mesmo:

```
🎬 assistir o vídeo  →  📝 anotar e responder as perguntas  →  🛠️ implementar junto  →  ✅ conferir o "pronto quando"
```

> Regra: só começa a próxima feature quando a anterior estiver mergeada na `develop` com o `./run-test.sh` verde.

| # | Feature | Por que nessa posição | Status |
|:-:|---|---|:-:|
| 1 | [Flyway](#1-flyway--migrações-de-banco) | base de tudo: a tabela de usuários do login já nasce como migração | 🛠️ |
| 2 | [Spring Security + JWT](#2-spring-security--jwt) | débito mais grave: hoje qualquer pessoa movimenta qualquer conta | ⬜ |
| 3 | [Testcontainers](#3-testcontainers--testes-com-mysql-real) | testa migrações, segurança e concorrência contra o MySQL de verdade | ⬜ |
| 4 | [Swagger / OpenAPI](#4-swagger--openapi) | documentação viva da API, já com o login no botão *Authorize* | ⬜ |
| 5 | [Deploy na AWS](#5-deploy-na-aws---futuro) 🔮 | **futuro**: ainda não sabemos como fazer | 🔮 |
| 6 | [Mensageria](#6-mensageria---futuro) 🔮 | **futuro**: ainda não sabemos como fazer | 🔮 |

Legenda: ⬜ não começou · 🎬 estudando · 🛠️ implementando · ✅ mergeado · 🔮 futuro (só pesquisa, sem plano de implementação)

> 🔮 **Features 1 a 4:** têm plano de implementação e entram na fila agora.
> **Features 5 e 6:** são **para o futuro**. Ainda não temos ideia de como fazer, então elas não têm passo a passo, só perguntas a responder e o que estudar. O plano de implementação só será escrito quando as features 1 a 4 estiverem prontas e o estudo tiver respondido as perguntas.

> ⚠️ O MVP vence em **31/10/2026** e o item "Colocar no ar" do roadmap continua valendo. Ele pode ser resolvido de um jeito mais simples que a AWS (decisão em aberto em `decisoes.md`) sem esperar a feature 5.

---

## 1. Flyway — migrações de banco

**Branch:** `feature/flyway` · **Depende de:** nada · **Fecha o débito:** `ddl-auto=update`

### 🎬 Antes de implementar
**Buscar no YouTube:** `Flyway Spring Boot MySQL` · `migrations Flyway Spring Boot` · `Flyway vs ddl-auto`
**Documentação:** https://documentation.red-gate.com/flyway

Durante o vídeo, prestar atenção em:
- [ ] O que é uma migração e por que ela é **versionada**
- [ ] O padrão de nome `V1__descricao.sql` (dois underlines)
- [ ] O que a tabela `flyway_schema_history` guarda
- [ ] Por que **nunca** se edita uma migração já aplicada
- [ ] A diferença entre `ddl-auto=update` e `ddl-auto=validate`

**Perguntas para responder sozinho antes de codar:**
1. Se eu mudar o `V1__` depois que ele já rodou no banco, o que acontece?
2. Como eu adiciono uma coluna nova sem perder os dados que já existem?
3. Por que o Hibernate passa a só *validar* o schema?

### 🛠️ Implementação
- [x] Criar a branch `feature/flyway` a partir da `develop` atualizada
- [x] Ver o schema que o Hibernate gera hoje no MySQL (`SHOW CREATE TABLE ...`)
- [x] Adicionar o Flyway no `pom.xml` (starter do Spring Boot 4 + módulo `flyway-mysql`)
- [x] Escrever `backend/src/main/resources/db/migration/V1__schema_inicial.sql`
  - [x] `tb_endereco`, `tb_cliente`, `tb_conta`, `tb_lancamento`
  - [x] constraints `uk_cliente_cpf`, `uk_cliente_email`, `uk_conta_numero` e as FKs
  - [x] `DECIMAL(19,2)` para dinheiro, coluna `versao` da conta
- [x] `spring.jpa.hibernate.ddl-auto=validate`
- [x] Fazer os testes em H2 rodarem a mesma migração (SQL portável: `BIT` sem tamanho e UUID fixado como `BINARY` no Hibernate; também foi preciso remover o `defer-datasource-initialization` do perfil de teste)
- [x] `V2__email_minusculo.sql`: migração de **dados** (e-mails existentes em minúsculas) + o service salvar em minúsculas
- [x] Atualizar `debitos-tecnicos.md`, `backend/README.md` e o roadmap

### ✅ Pronto quando
- [x] `./run-test.sh` verde (109 testes)
- [x] App sobe com `docker compose down -v` + banco vazio e cria tudo pela migração (`Successfully applied 2 migrations`, smoke 17/17)
- [x] `flyway_schema_history` com `V1` e `V2`
- [ ] PR revisado e mergeado na `develop`

### 📝 Minhas anotações

> Resumo para entrevista (implementado em 15/10/2026):
>
> Troquei o `ddl-auto=update` pelo **Flyway**: o schema agora nasce de scripts versionados (`V1__schema_inicial`,
> `V2__email_minusculo`) e o Hibernate só **valida** as entidades contra o banco, impedindo a subida se divergirem.
> O `V1` reproduz o que o Hibernate gerava, mas com nomes legíveis nas constraints e índices para o extrato. O `V2` é uma
> migração de **dados**: deixa os e-mails em minúsculas, e o service passou a normalizar os novos cadastros.
>
> O maior aprendizado foi o **H2 × MySQL**: o `defer-datasource-initialization` fazia o JPA validar antes de o Flyway criar
> as tabelas; o H2 não aceita `BIT(1)`, espera UUID nativo (resolvi fixando `preferred_uuid_jdbc_type=BINARY`, o mesmo que o
> MySQL já usava) e trata `CAST(... AS BINARY)` diferente. Vi na prática a regra de ouro: um comentário num `V1` já aplicado
> gera *checksum mismatch* e a aplicação não sobe. Sem produção ainda, recriei o banco local em vez de usar *baseline*.

---

## 2. Spring Security + JWT

**Branch:** `feature/spring-security-jwt` · **Depende de:** Flyway · **Fecha os débitos:** sem login e `GET /clientes` expondo dados pessoais

### 🎬 Antes de implementar
**Buscar no YouTube:** `Spring Security JWT Spring Boot` · `Spring Security filter chain explicado` · `OAuth2 Resource Server JWT Spring Boot` · `BCrypt senha Spring`
**Documentação:** https://docs.spring.io/spring-security/reference/

Durante o vídeo, prestar atenção em:
- [ ] Autenticação (quem é você?) × autorização (o que você pode fazer?)
- [ ] `401 Unauthorized` × `403 Forbidden`
- [ ] Como a `SecurityFilterChain` decide quais rotas são públicas
- [ ] Por que a senha vai com **BCrypt** (hash com salt) e nunca em texto
- [ ] As 3 partes do JWT (header, payload, assinatura) e por que ele é **stateless**
- [ ] Expiração do token e o que é refresh token
- [ ] As duas formas de validar o JWT: filtro próprio × **OAuth2 Resource Server** (a oficial do Spring)

**Perguntas para responder sozinho antes de codar:**
1. Se alguém alterar o payload do token, por que a API percebe?
2. Onde fica guardada a "sessão" num sistema stateless?
3. Como garantir que o cliente A não consegue sacar da conta do cliente B, mesmo com um token válido?

### 🛠️ Implementação
- [ ] Decidir com o time: login com CPF + senha? (registrar em `decisoes.md`)
- [ ] `V3__usuarios.sql`: senha (hash BCrypt) e perfil ligados ao cliente
- [ ] Cadastro de cliente recebendo a senha (nunca devolver a senha no response)
- [ ] `POST /auth/login` devolvendo o JWT com expiração
- [ ] `SecurityFilterChain`: públicas só `POST /clientes` e `POST /auth/login`
- [ ] Validação do token em todas as outras rotas
- [ ] **Regra de dono:** depósito, saque, transferência e extrato só nas contas do usuário logado → `403`
- [ ] `GET /clientes` só para perfil admin (ou removido do uso público)
- [ ] `401` e `403` no mesmo formato de erro do `ControllerExceptionHandler`
- [ ] Segredo do JWT em variável de ambiente (`.env`), nunca no código
- [ ] Testes: login ok, senha errada, sem token, token expirado, conta de outro cliente
- [ ] Combinar com o Renan as mudanças no frontend (guardar e enviar o token)
- [ ] Atualizar `README_CONTRATO_API.md`, `README_TESTES_E2E.md` e os débitos

### ✅ Pronto quando
- [ ] `./run-test.sh` verde
- [ ] Sem token → `401`; conta de outro cliente → `403`
- [ ] Nenhuma senha aparece em log, response ou banco em texto puro
- [ ] PR revisado e mergeado na `develop`

### 📝 Minhas anotações


---

## 3. Testcontainers — testes com MySQL real

**Branch:** `feature/testcontainers` · **Depende de:** Flyway (o container recebe o schema pela migração) · **Fecha o débito:** testes de controller sem `MockMvc`

### 🎬 Antes de implementar
**Buscar no YouTube:** `Testcontainers Spring Boot` · `ServiceConnection Testcontainers` · `testes de integração Spring Boot MySQL`
**Documentação:** https://java.testcontainers.org/ · https://docs.spring.io/spring-boot/reference/testing/testcontainers.html

Durante o vídeo, prestar atenção em:
- [ ] Teste unitário × teste de integração
- [ ] Como o container sobe e é derrubado durante o teste
- [ ] O que o `@ServiceConnection` configura sozinho
- [ ] Como reaproveitar o mesmo container entre classes (teste mais rápido)
- [ ] O que é preciso para rodar no GitHub Actions

**Perguntas para responder sozinho antes de codar:**
1. Por que o bug do H2 2.4.240 não teria acontecido com Testcontainers?
2. Quando ainda vale a pena usar H2?
3. Como provar num teste que o lock otimista funciona com duas requisições ao mesmo tempo?

### 🛠️ Implementação
- [ ] Dependências `spring-boot-testcontainers` + módulo MySQL do Testcontainers
- [ ] Classe base de teste com MySQL 8.4 (a mesma versão do `docker-compose.yml`)
- [ ] Rodar os testes de repositório contra o MySQL real (com as migrações do Flyway)
- [ ] Teste de concorrência: 2 threads transferindo da mesma conta → um `409` e o saldo certo
- [ ] Testes de rota com `MockMvc`: cadastro, abertura de conta, transferência, extrato e login
- [ ] `RkfbankApplicationTests` sem precisar do MySQL do Docker ligado (fecha um débito 🔴)
- [ ] Conferir que o `backend-ci.yml` roda os testes com Docker
- [ ] Atualizar `backend/README.md` (seção Testes) e os débitos

### ✅ Pronto quando
- [ ] `./run-test.sh` e o CI verdes
- [ ] Pelo menos 1 teste de concorrência real passando
- [ ] PR revisado e mergeado na `develop`

### 📝 Minhas anotações


---

## 4. Swagger / OpenAPI

**Branch:** `feature/swagger` · **Depende de:** Spring Security (para documentar o login)

### 🎬 Antes de implementar
**Buscar no YouTube:** `springdoc openapi Spring Boot` · `Swagger UI JWT Authorize`
**Documentação:** https://springdoc.org/

- [ ] O que é a especificação OpenAPI e o que o Swagger UI faz com ela
- [ ] Como documentar request, response e erros
- [ ] Como configurar o botão *Authorize* com o JWT

### 🛠️ Implementação
- [ ] Dependência do springdoc compatível com o Spring Boot 4 (conferir a versão)
- [ ] Liberar `/swagger-ui` e `/v3/api-docs` na `SecurityFilterChain`
- [ ] Descrever as rotas, os DTOs e os status de erro (`400`, `401`, `403`, `404`, `409`, `422`, `503`)
- [ ] Esquema de segurança Bearer JWT
- [ ] Link do Swagger no `README.md` e no contrato da API

### ✅ Pronto quando
- [ ] Dá para fazer o fluxo completo (cadastro → login → depósito → extrato) só pelo Swagger UI
- [ ] PR revisado e mergeado na `develop`

### 📝 Minhas anotações


---

## 5. Deploy na AWS — 🔮 futuro

> 🔮 **Feature para o futuro.** Ainda **não sabemos como fazer** e não existe plano de implementação. Esta seção é só um guia de pesquisa: primeiro estudar e responder as perguntas, depois escrever o passo a passo aqui, junto com o time.

**Quando revisitar:** depois que as features 1 a 4 estiverem mergeadas.

### 🧭 O que já sabemos
- Queremos a API, o banco e o site acessíveis por um link público.
- A API não pode ir para o ar aberta: precisa do login (feature 2) antes.
- O banco no ar precisa nascer pelas migrações do Flyway (feature 1).
- Nenhuma senha ou segredo pode ficar no repositório.

### ❓ O que ainda não sabemos (responder antes de planejar)
- [ ] AWS é mesmo a melhor opção para nós, ou um serviço mais simples resolve o MVP? Quanto custa cada opção?
- [ ] Como empacotar a aplicação para rodar fora da nossa máquina (Docker? `.jar`?)
- [ ] Onde o MySQL vai rodar e como fazer backup dele?
- [ ] Onde ficam as senhas e o segredo do JWT em produção?
- [ ] Como o frontend do Renan vai ser publicado e falar com a API?
- [ ] Como evitar uma conta surpresa no fim do mês?
- [ ] Quem do time vai cuidar do ambiente no ar?

### 📚 Para estudar quando chegar a hora
**Buscar no YouTube:** `deploy Spring Boot iniciante` · `Spring Boot Docker` · `AWS para iniciantes` · `AWS Free Tier custos`
**Documentação:** https://docs.aws.amazon.com/

- [ ] Conceitos básicos de nuvem (o curso de Cloud Practitioner ajuda aqui)
- [ ] O que é uma imagem Docker de aplicação
- [ ] Opções de hospedagem e a diferença entre elas
- [ ] Variáveis de ambiente e gerenciamento de segredos
- [ ] Alertas de custo

### 🛠️ Plano de implementação
*A definir depois do estudo.* Quando as perguntas acima estiverem respondidas, escrever aqui o passo a passo e o "pronto quando", no mesmo formato das features 1 a 4.

### 📝 Minhas anotações


---

## 6. Mensageria — 🔮 futuro

> 🔮 **Feature para o futuro.** Ainda **não sabemos como fazer** e não existe plano de implementação. Esta seção é só um guia de pesquisa: primeiro estudar e responder as perguntas, depois escrever o passo a passo aqui, junto com o time.

**Quando revisitar:** depois que as features 1 a 4 estiverem mergeadas e o deploy (5) estiver definido.

### 🧭 O que já sabemos
- Hoje tudo acontece dentro de uma única requisição: a transferência grava débito, crédito e lançamento na mesma transação.
- A ideia é que algumas coisas possam acontecer **depois**, sem travar o cliente (exemplo: gerar comprovante, avisar o cliente).
- O Kevin já usou eventos internos do Spring no projeto BookMarket (compra disparando a nota fiscal).

### ❓ O que ainda não sabemos (responder antes de planejar)
- [ ] O RKF Bank **precisa** de mensageria, ou é só para aprender? Qual problema real ela resolve aqui?
- [ ] Qual seria o primeiro caso de uso (comprovante? notificação? Pix?)
- [ ] Evento interno do Spring já resolve, ou precisa de uma fila de verdade?
- [ ] Qual ferramenta: RabbitMQ, Kafka ou outra? Por quê?
- [ ] O que acontece se a mensagem se perder, ou chegar duas vezes?
- [ ] Como testar isso de forma automática?
- [ ] Quanto isso aumenta o custo e a complexidade do deploy?

### 📚 Para estudar quando chegar a hora
**Buscar no YouTube:** `mensageria para iniciantes` · `Spring events` · `RabbitMQ Spring Boot` · `RabbitMQ vs Kafka`
**Documentação:** https://docs.spring.io/spring-amqp/reference/

- [ ] O que é comunicação síncrona × assíncrona
- [ ] Eventos internos do Spring
- [ ] O que é uma fila e um broker de mensagens
- [ ] Consistência eventual
- [ ] Mensagem perdida e mensagem duplicada (só entender o problema; a solução vem depois)

### 🛠️ Plano de implementação
*A definir depois do estudo.* Quando as perguntas acima estiverem respondidas, escrever aqui o passo a passo e o "pronto quando", no mesmo formato das features 1 a 4.

### 📝 Minhas anotações
