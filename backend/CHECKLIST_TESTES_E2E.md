# ✅ RKF Bank — Checklist de testes E2E

Checklist de validação do MVP, com o resultado da última execução completa.
Como executar cada passo: [README_TESTES_E2E.md](README_TESTES_E2E.md). Rotas e formatos: [contrato da API](README_CONTRATO_API.md).

## Última execução

| | |
|---|---|
| **Data** | 13/10/2026 |
| **Branch** | `feature/implementando-logs` |
| **Ambiente** | Java 25.0.4.1 · Spring Boot 4.1.1 · MySQL 8.4 (Docker) · perfil padrão · ViaCEP real |
| **Resultado** | **38/38 itens aprovados** |

> Os dados de teste (CPF, e-mail e número de conta) foram gerados na hora para não colidir com o que já estava no banco.

> 🔁 **20/10/2026:** contrato 2.1 (idempotência, número de conta gerado, `429` no login). O aceite do MVP passa a seguir o
> **guia privado da equipe** (seção 11, fora do repositório), com as contas do Felipe e do Renan; quem não é da equipe usa o
> [guia público](README_TESTES_E2E.md) com as contas de visitante. A próxima execução completa atualiza esta página.

Legenda: ✅ aprovado · ❌ reprovado · ⏳ não executado

---

## 👤 1. Clientes (`/clientes`)

| # | Tipo | Item | Esperado | Resultado |
|---|---|---|---|:---:|
| 1.1 | Sucesso | Criação de cliente origem e destino | `201` com `id` (UUID) e os dados enviados | ✅ |
| 1.2 | Sucesso | Endereço via CEP | `endereco` com `logradouro`, `bairro`, `cidade`, `uf` e `ibge` vindos do ViaCEP | ✅ |
| 1.3 | Sucesso | Formatação na resposta | `cpf` como `000.000.000-00`, `telefone` como `(11) 99999-8888`, `cep` como `00000-000` | ✅ |
| 1.4 | Erro | CPF inválido (`11111111111`) | `400`, `campos[0].campo = "cpf"`, `campos[0].mensagem = "cpf inválido"` | ✅ |
| 1.5 | Sucesso | Listagem não paginada (`paginado=false` ou sem parâmetro) | `200` com array (`[...]`) | ✅ |
| 1.6 | Sucesso | Listagem paginada (`paginado=true`) | `200` com `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last` | ✅ |
| 1.7 | Erro | Paginação inválida (`page < 0` ou `size` fora de 1–100) | `400` com o campo (`page` ou `size`) em `campos` | ✅ |

## 🏦 2. Contas (`/contas`)

| # | Tipo | Item | Esperado | Resultado |
|---|---|---|---|:---:|
| 2.1 | Sucesso | Abertura de conta origem e destino (`POST /contas/clientes/{clienteId}`) | `201`, `saldo: 0.00`, `ativa: true`, `clienteId` correto | ✅ |
| 2.2 | Erro | Abertura para cliente inexistente (`POST /contas/clientes/{uuidAleatorio}`) | `404` `Cliente não encontrado` | ✅ |
| 2.3 | Sucesso | Listagem de contas, paginada e não paginada | array e objeto `Page`, mesma lógica de clientes | ✅ |

## 💸 3. Operações financeiras (`/contas/{id}/deposito`, `/contas/{id}/saque`, `/contas/transferencia`)

| # | Tipo | Item | Esperado | Resultado |
|---|---|---|---|:---:|
| 3.1 | Sucesso | Depósito de 150.00 | `204` sem corpo; saldo vai a 150.00 | ✅ |
| 3.2 | Erro | Depósito em conta inexistente | `404` `Conta nao encontrada` | ✅ |
| 3.3 | Sucesso | Saque de 20.00 | `204`; saldo vai a 130.00 | ✅ |
| 3.4 | Erro | Saque maior que o saldo | `422` `Saldo insuficiente` | ✅ |
| 3.5 | Sucesso | Transferência de 50.00 (`contaOrigemId`, `contaDestinoId`, `valor`, `descricao`) | `204` sem corpo | ✅ |
| 3.6 | Consistência | Saldos após depósito 150 → saque 20 → transferência 50 | origem **80.00**, destino **50.00** | ✅ |

## 📜 4. Extrato (`/contas/{contaId}/lancamentos`)

| # | Tipo | Item | Esperado | Resultado |
|---|---|---|---|:---:|
| 4.1 | Sucesso | Extrato da conta origem | `200` com as 3 operações | ✅ |
| 4.2 | Regra | Ordem cronológica | decrescente por `dataHora`: `TRANSFERENCIA` → `SAQUE` → `DEPOSITO` | ✅ |
| 4.3 | Regra | Tipos e vínculos | transferência: origem e destino preenchidos · saque: só `contaOrigemId` (`contaDestinoId = null`) · depósito: só `contaDestinoId` (`contaOrigemId = null`) | ✅ |
| 4.4 | Regra | Extrato da conta destino | 1 `TRANSFERENCIA`, com `contaDestinoId` = conta destino (entrada) | ✅ |

## 🚫 5. Cenários de erro do roteiro

Numeração igual à seção 2 do [roteiro](README_TESTES_E2E.md#7-cenários-de-erro-e01-a-e18). E1, E6, E12, E15 e E17 já estão
cobertos acima (itens 1.4, 2.2, 3.4, 3.2 e 1.7).

| # | Cenário | Esperado | Resultado |
|---|---|---|:---:|
| E2 | Campo obrigatório vazio (`nomeCompleto: ""`) | `400`, campo `nomeCompleto` | ✅ |
| E3 | CPF repetido | `409` `CPF ja cadastrado` | ✅ |
| E4 | E-mail repetido | `409` `Email ja cadastrado` | ✅ |
| E5 | CEP inexistente (`99999999`) | `422` `CEP não encontrado` | ✅ |
| E7 | Número de conta repetido | `409` `Numero de conta ja existe` | ✅ |
| E8 | Agência inválida (`"1"`) | `400`, campo `agencia` | ✅ |
| E9 | Tipo de conta inválido (`SALARIO`) | `400` `JSON invalido ou tipo de campo incompativel` | ✅ |
| E10 | Valor zero | `400`, campo `valor` | ✅ |
| E11 | Três casas decimais (`10.555`) | `400`, campo `valor` | ✅ |
| E13 | Transferência maior que o saldo | `422` `Saldo insuficiente para transferencia` | ✅ |
| E14 | Transferência para a mesma conta | `422` `Conta de origem e destino nao podem ser iguais` | ✅ |
| E16 | ID inválido na URL (`/contas/abc/lancamentos`) | `400` `Parametro de URL invalido` | ✅ |
| E18 | JSON quebrado | `400` | ✅ |
| — | Nenhum erro acima altera saldo | origem continua em 80.00 | ✅ |

## 🔎 6. Logs

| # | Item | Esperado | Resultado |
|---|---|---|:---:|
| 6.1 | `X-Request-Id` | resposta devolve o id enviado no header | ✅ |
| 6.2 | Rastreio da chamada | todas as linhas da transferência levam o mesmo `[requestId]`, incluindo `FiltroLogRequisicao` e `ContaService` | ✅ |
| 6.3 | Dados pessoais | CPF e e-mail não aparecem em nenhuma linha de log | ✅ |
| 6.4 | Nível das recusas | recusas de negócio (saldo insuficiente, CPF repetido) saem em `WARN` | ✅ |

---

## 🛫 7. Banco (Flyway)

Executado em **15/10/2026**, na branch `feature/flyway`, com MySQL 8.4 (Docker) e o perfil padrão.

| # | Tipo | Item | Esperado | Resultado |
|---|---|---|---|:---:|
| 7.1 | Sucesso | API sobe num banco vazio | log `Successfully applied 2 migrations ... now at version v2` | ✅ |
| 7.2 | Sucesso | Histórico das migrações | `flyway_schema_history` com `1 - schema inicial` e `2 - email minusculo`, `success = 1` | ✅ |
| 7.3 | Sucesso | Hibernate valida o schema | API sobe com `ddl-auto=validate` sem erro de *Schema validation* | ✅ |
| 7.4 | Sucesso | `V2` corrige e-mails antigos | `Teste.Caixa@RKFbank.com` vira `teste.caixa@rkfbank.com`; nenhuma maiúscula restante | ✅ |
| 7.5 | Sucesso | Cadastro novo com e-mail em maiúsculas | salvo em minúsculas (coberto por `ClienteServiceTest`) | ✅ |
| 7.6 | Erro | Banco antigo, criado pelo Hibernate | API **recusa** subir com *Found non-empty schema(s) ... but no schema history table* | ✅ |
| 7.7 | Erro | Migração aplicada editada (um comentário no `V1`) | API **recusa** subir com *Migration checksum mismatch for migration version 1* | ✅ |
| 7.8 | Sucesso | Fluxo completo no schema criado pelo Flyway | `node scripts/smoke-e2e.mjs` → 17/17 | ✅ |

## 🧾 Correções em relação à versão anterior do checklist

O primeiro rascunho deste checklist tinha alguns pontos que não batiam com a API:

| Antes | Correto |
|---|---|
| `POST /clientes/{uuid}/contas` | `POST /contas/clientes/{uuid}` |
| `POST /transferencias` | `POST /contas/transferencia` |
| mensagem `"cpf invalido"` | `"cpf inválido"` |
| "paginação inválida lança a exceção esperada" | `400 Bad Request` com o campo inválido em `campos` |
| ordem do extrato e vínculos dos lançamentos sem resultado | executados e aprovados (4.2 a 4.4) |

## ⏳ Fora deste checklist

- Fluxo pelo frontend (`http://localhost:3000/cadastro`) — validar manualmente.
- Conflito de concorrência (`409` por *lock* otimista) — coberto só pelos testes automatizados
  (`ControllerExceptionHandlerTest` e `ContaRepositoryTest`).
