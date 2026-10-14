# Como contribuir — RKF Bank

Este guia mostra como o time trabalha: do pegar uma tarefa até o merge.

> **Regra de ouro:** se não está escrito, não foi combinado.
> Decisões vão para [planing/decisoes.md](planing/decisoes.md). Mudanças na API vão para o
> [contrato](backend/README_CONTRATO_API.md).

---

## 1. Passo a passo de uma tarefa

1. Escolha um item do [roadmap](planing/roadmap.md).
2. Vai criar ou mudar um endpoint? Atualize o **contrato** primeiro e avise o outro dev.
3. Crie uma branch a partir da `develop`.
4. Faça o código **com testes**.
5. Rode o checklist local (seção 4).
6. Abra um Pull Request para a `develop`.
7. Depois da revisão e do merge, marque `[x]` no roadmap.

---

## 2. Branches

| Branch | Para quê |
|---|---|
| `main` | versão estável (o que vai para o ar) |
| `develop` | onde o trabalho do time se junta |
| `feature/nome` | funcionalidade nova |
| `fix/nome` | correção de bug |
| `refactor/nome` | melhoria no código sem mudar o comportamento |
| `docs/nome` | documentação |
| `chore/nome` | configuração, dependências, build |

Ninguém faz commit direto na `main` ou na `develop`: tudo entra por Pull Request.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/tela-extrato
```

Dica: branch pequena e curta. Quanto mais tempo ela fica aberta, mais conflito aparece.

---

## 3. Commits

Usamos o padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br):

```text
tipo: o que foi feito
```

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feat` | funcionalidade nova | `feat: adiciona tela de extrato` |
| `fix` | correção de bug | `fix: impede saque maior que o saldo` |
| `refactor` | melhoria interna | `refactor: separa validação de paginação` |
| `test` | testes | `test: cobre transferência para a mesma conta` |
| `docs` | documentação | `docs: atualiza contrato da API` |
| `chore` | configuração | `chore: atualiza dependências` |

Nunca faça commit de `.env`, senhas ou tokens.

---

## 4. Antes de abrir o Pull Request

**Backend** (na raiz do projeto, com o banco ligado):

```bash
./mvnw clean verify
```

**Frontend** (dentro de `frontend/`):

```bash
pnpm lint
pnpm typecheck
pnpm build
```

Tudo verde? Pode abrir o PR.

---

## 5. Pull Request

Use este modelo na descrição:

```markdown
## O que foi feito
Resumo em poucas linhas.

## Como testar
1. Passo a passo para quem vai revisar.

## Checklist
- [ ] Testes passando
- [ ] Contrato da API atualizado (se mexeu em endpoint)
- [ ] Prints da tela (se mexeu no visual)
```

Um PR bom é pequeno: dá para revisar em até 20 minutos.

---

## 6. Revisão de código

Quem revisa confere:

- [ ] Faz o que a tarefa pede?
- [ ] Tem teste para o caminho certo **e** para pelo menos um erro?
- [ ] Os nomes são claros?
- [ ] Cada camada faz só o seu trabalho? (controller sem regra de negócio; componente sem `fetch`)
- [ ] Nenhum dado pessoal (CPF, e-mail) aparece em log sem máscara?

Comentários devem ser claros e respeitosos. Use `nit:` para sugestões que não são obrigatórias.

---

## 7. Padrões de código

- Nomes do negócio em **português** (`Conta`, `depositar`, `useCadastroForm`).
- Comentário explica o **porquê**, não o quê.
- Código que não é usado é apagado (o Git guarda o histórico).
- Detalhes do backend: [backend/README_IMPLEMENTACAO.md](backend/README_IMPLEMENTACAO.md).
- Detalhes do frontend: [frontend/README.md](frontend/README.md).
