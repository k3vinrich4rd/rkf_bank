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
| `build` | build e dependências do Maven | `build: fixa H2 em 2.3.232` |
| `ci` | workflows do GitHub Actions | `ci: adiciona CI do frontend` |

Escopo é opcional: `test(repositories): cobre o extrato`, `fix(deps): atualiza Tomcat`.

Nunca faça commit de `.env`, senhas ou tokens.

> **Hook de commit:** rode `scripts/instalar-hooks.sh` uma vez depois de clonar. O Git passa a recusar
> mensagens fora do padrão acima (merges e reverts gerados pelo Git passam direto).

---

## 4. Antes de abrir o Pull Request

Atalho para os dois lados (mostra só o resumo; o log completo fica em `.validar/*.log`):

```bash
scripts/validar.sh backend     # ou: frontend | tudo
```

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

Mexeu em regra de negócio, rota ou banco? Com a API no ar, rode também o smoke test: `node scripts/smoke-e2e.mjs`.
Qual teste rodar para cada tipo de mudança: ["Mudei algo: quais testes rodar?"](backend/README.md#-mudei-algo-quais-testes-rodar).

> **Mudou uma entidade ou o banco?** O schema é do **Flyway**: crie uma migração nova em
> `backend/src/main/resources/db/migration` (`V<n>__descricao.sql`). Nunca edite uma migração que já entrou na `develop`.

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

---

## 8. Ferramentas e agentes de IA

O projeto tem contexto e skills para o Claude Code (opcional; nada aqui é obrigatório para contribuir):

| Arquivo | Para quê |
|---|---|
| [CLAUDE.md](CLAUDE.md) | contexto do projeto inteiro: comandos, regras e armadilhas conhecidas |
| [frontend/CLAUDE.md](frontend/CLAUDE.md) | regras específicas do frontend |
| `.claude/skills/implementar-feature` | `/implementar-feature <n>`: implementa uma feature do [checklist](planing/checklist-features.md) explicando cada conceito |
| `.claude/skills/entregar` | `/entregar`: valida, atualiza docs, commita, faz push e escreve o PR |
| `.claude/skills/verificar-contrato` | `/verificar-contrato`: compara backend, contrato e tipos do frontend |
| `.claude/skills/varrer-vulnerabilidades` | `/varrer-vulnerabilidades`: CVEs no Maven e no pnpm e a correção mínima |
| `.claude/skills/testar-e2e` | `/testar-e2e`: fluxo real contra a API rodando |
| `.claude/skills/explicar-codigo` | `/explicar-codigo <alvo>`: explica um código existente, sem alterar nada |
| `.claude/skills/revisar-migracao` | `/revisar-migracao`: revisa as migrações Flyway antes do commit |
| `.claude/skills/nova-tela` | `/nova-tela <nome>`: cria uma tela no padrão do frontend |
| `.claude/skills/recado` | `/recado`: escreve, responde ou lista recados do time |
| `.claude/skills/notas-de-versao` | `/notas-de-versao`: notas de versão entre `main` e `develop` |

Os scripts em [`scripts/`](scripts) funcionam sem IA: `validar.sh`, `smoke-e2e.mjs`, `varrer-cves.js` e o hook de commit.
