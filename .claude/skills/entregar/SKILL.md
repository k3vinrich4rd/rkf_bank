---
name: entregar
description: Fecha uma entrega no RKF Bank - atualiza a branch com a develop, valida backend e/ou frontend com os scripts, confere a documentação afetada, organiza commits semânticos, faz push, abre o PR para a develop, espera o CI e faz o merge na develop, deixando o checkout na develop. A main só recebe o que for aprovado na reunião semanal do Kevin com o Renan. Use quando o usuário disser "/entregar", "fecha o PR", "commita e sobe" ou similar.
---

# /entregar

Objetivo: levar o trabalho da branch até a `develop` (PR aberto, CI verde e merge feito), gastando o mínimo de tokens.

```text
feature/x ──(atualiza com a develop)──► PR feature/x → develop ──(CI verde)──► merge ──► checkout fica na develop
                                                                                            │
                         reunião semanal (Kevin + Renan, fim da semana) ──► /notas-de-versao ──► PR develop → main
```

> 🔒 **Esta é a única skill que commita, faz push e mergeia, e só com ordem explícita do Kevin na mensagem atual.**
> - `/entregar` digitado por ele, ou "commita e sobe" → commita, faz push, abre o PR para a `develop` e, com o CI verde, **mergeia na `develop`**.
> - "commita" sem falar em push → só commita.
> - Chegou aqui por sugestão de outra skill, ou o pedido é ambíguo → **pare** antes do passo 4, mostre o plano de commits e pergunte.
> - **Nunca** abra PR para a `main` nem mergeie nela: isso só acontece depois da reunião semanal, quando o Kevin pedir.
> - Nunca `reset`, `revert`, `rebase` ou `push --force` sem pedido explícito.
> - Várias branches para entregar? Uma de cada vez, na ordem combinada: entregue a primeira até o merge, e só então
>   atualize a próxima com a `develop` nova (passo 1) antes de abrir o PR dela.

## 1. Entender o que mudou
- `git status --short` e `git diff --stat origin/develop...HEAD` (mais o que não foi commitado).
- Classifique: mudou `backend/` ou `pom.xml`? mudou `frontend/`? só docs?
- Se a branch atual for `main` ou `develop`, **pare**: crie uma branch `feature/`, `fix/`, `docs/` ou `chore/` antes.
- **Atualize com a `develop` antes de validar:** `git fetch` + `git merge origin/develop` (merge, não rebase). Conflito → pare e mostre.
- `git log --oneline origin/develop..HEAD` mostra commits de outra feature ainda não mergeada (branch empilhada)? **Pare** e avise: o PR carregaria trabalho alheio. Espere o merge da outra e atualize.

## 2. Validar (script, não IA)
- Backend mudou → `scripts/validar.sh backend`.
- Frontend mudou → `scripts/validar.sh frontend`.
- Mudou regra de negócio, rota ou banco → também `./run-test.sh` (**avise antes: apaga o MySQL local**).
- Se falhar: mostre o resumo do script, explique a causa e corrija **só se for parte do trabalho atual**. Nunca afrouxe teste.

## 3. Documentação afetada
Confira e atualize só o que a mudança tornou falso:

| Se mudou... | Atualize |
|---|---|
| rota, request ou response | `backend/README_CONTRATO_API.md` (+ avisar que `frontend/lib/api/types.ts` precisa acompanhar) |
| quantidade/classes de teste | tabela de testes em `backend/README.md` e `README.md` |
| resolveu um débito | apague a linha em `planing/debitos-tecnicos.md` |
| concluiu item do roadmap ou do checklist | `planing/roadmap.md` / `planing/checklist-features.md` |
| decisão de arquitetura | `planing/decisoes.md` |

## 4. Commits semânticos
- Um assunto por commit: `build:` (pom, dependências), `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`, `ci:`. Escopo opcional: `test(repositories): ...`.
- Mensagem em português, no imperativo do time ("adiciona", "corrige", "atualiza").
- O hook `scripts/hooks/commit-msg` valida o padrão (se não estiver ativo, rode `scripts/instalar-hooks.sh`).
- Termine cada mensagem com a linha de coautoria definida pelo ambiente, quando houver.

## 5. Push e PR para a `develop`
- Logo antes do push, atualize de novo: `git fetch` + `git merge origin/develop` (alguém pode ter mergeado enquanto você
  validava). Entrou algo novo? Rode a validação do passo 2 outra vez. Conflito → pare e mostre.
- `git push -u origin <branch>` só se a ordem da mensagem atual incluir push (ver o quadro 🔒 no topo).
- Escreva a descrição do PR no modelo do `CONTRIBUTING.md` ("O que foi feito", "Como testar", "Checklist"), com:
  - o resultado real da validação (ex.: `107 testes, 0 falhas`);
  - o que o revisor deve olhar com atenção (o Renan revê tudo na reunião semanal);
  - conflitos previsíveis com outras branches abertas (ex.: as duas mexem no `<properties>` do `pom.xml`).
- Abra com `gh pr create --base develop --head <branch> --title "<tipo>(escopo): ..." --body-file <arquivo>`.
  **Base sempre `develop`.**

## 6. CI e merge na `develop`
- Espere os checks: `gh pr checks <número> --watch`. Algum vermelho → **não mergeie**: mostre o check que falhou,
  corrija na mesma branch (novo commit, push) e espere de novo.
- PR só de docs ou só de frontend pode não ter check nenhum (o Backend CI só roda quando muda `backend/` ou `pom.xml`):
  aí vale a validação local do passo 2, e o resultado dela entra na descrição do PR.
- Verde → `gh pr merge <número> --merge --delete-branch` (merge commit, como o resto do histórico; nada de squash,
  que perde a ancestralidade entre `develop` e `main`).
- Deixe o checkout na `develop` atualizada: `git switch develop` + `git pull --ff-only origin develop`.
- Branch seguinte da fila? Volte ao passo 1 nela (`git merge origin/develop` traz o que acabou de entrar).

## 7. Fechar
- Mostre: link do PR, resultado do CI, commit do merge na `develop` e em que branch o checkout ficou.
- Lembre que a `main` fica para a reunião semanal: lá, `/notas-de-versao` mostra o que vai da `develop` para a `main`,
  e o PR `develop → main` só é aberto quando o Kevin pedir depois da reunião.
- Mudou rota ou formato da API? Sugira o `/recado` para o Renan.
