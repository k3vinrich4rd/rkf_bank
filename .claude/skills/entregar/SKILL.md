---
name: entregar
description: Fecha uma entrega no RKF Bank seguindo o Git Flow do time - develop atualizada, branch feature nascida dela, merge da develop na feature (git pull) com os conflitos resolvidos, validação por script, commits semânticos, PR feature → develop com CI verde e merge, e por fim o PR develop → main deixado aberto (o merge na main é do Kevin e do Renan). Use quando o usuário disser "/entregar", "fecha o PR", "commita e sobe" ou similar.
---

# /entregar

Objetivo: levar o trabalho da branch até a `develop` (PR, CI verde e merge) e deixar a `develop` pronta para a `main`
num PR aberto, gastando o mínimo de tokens.

## 🔀 O Git Flow do time (sempre este, em toda mudança nova)

```text
1. atualizar a develop        git switch develop && git pull origin develop
2. criar a branch feature     git switch -c feature/x            (fix/, docs/, chore/ também)
        ... trabalho e commits na feature ...
3. trazer a develop de novo   git fetch && git pull origin develop   (na feature: merge da develop nela)
4. resolver conflitos         na feature, validar de novo e commitar a resolução
5. feature → develop          PR com base develop, CI verde, merge (--merge)
6. develop → main             PR aberto (ou atualizado) e PARA AÍ: o merge na main é do Kevin e do Renan
```

> 🔒 **Esta é a única skill que commita, faz push e mergeia, e só com ordem explícita do Kevin na mensagem atual.**
> - `/entregar` digitado por ele, ou "commita e sobe" → commita, faz push, abre o PR para a `develop`, mergeia com o
>   CI verde e deixa o PR `develop → main` aberto.
> - "commita" sem falar em push → só commita.
> - Chegou aqui por sugestão de outra skill, ou o pedido é ambíguo → **pare** antes do passo 4, mostre o plano de commits e pergunte.
> - **Nunca mergeie na `main`** (nem com `gh pr merge`, nem com push direto): só abra ou atualize o PR `develop → main`.
> - Nunca `reset`, `revert`, `rebase` ou `push --force` sem pedido explícito. Atualizar é sempre **merge** (`git pull` sem `--rebase`).
> - Várias branches para entregar? Uma de cada vez: entregue a primeira até o merge na `develop`, e só então volte ao
>   passo 1 da próxima (ela precisa receber a `develop` nova).

## 1. Partir da develop atualizada
- Trabalho ainda não começou (ou está solto na `develop`)? `git switch develop` + `git pull origin develop` e crie a branch
  dela: `git switch -c <tipo>/<nome>` (mudanças não commitadas vão junto). **Nunca** commite direto na `develop` ou na `main`.
- Já está numa branch? `git status --short` e `git diff --stat origin/develop...HEAD` (mais o que não foi commitado).
- Classifique: mudou `backend/` ou `pom.xml`? mudou `frontend/`? só docs?
- `git log --oneline origin/develop..HEAD` mostra commits de outra feature ainda não mergeada (branch empilhada)? **Pare** e avise: o PR carregaria trabalho alheio. Espere o merge da outra.

## 2. Trazer a develop e resolver conflitos
- Com o trabalho commitado (ou guardado no passo 4 primeiro), na branch: `git fetch` + `git pull origin develop`
  (é um merge da `develop` na feature).
- **Conflito → resolva**, entendendo os dois lados: mantenha o que a `develop` trouxe **e** o que a feature fez.
  Arquivos gerados (`assets/readme/*.svg`, números do README, coleção do Insomnia) → resolva rodando o gerador de novo
  (`node scripts/gerar-svgs-readme.mjs`, `node scripts/gerar-colecao-insomnia.mjs`). Migração Flyway com o mesmo número
  nos dois lados → renumere a **da feature** (a da `develop` já foi aplicada). Commit da resolução:
  `git commit` (mensagem padrão do merge) e mostre ao Kevin o que foi decidido em cada arquivo.
- Conflito em que os dois lados mudam a mesma regra de negócio de jeitos diferentes → **pare** e pergunte qual vale.

## 3. Validar (script, não IA)
- Backend mudou → `scripts/validar.sh backend`.
- Frontend mudou → `scripts/validar.sh frontend`.
- Mudou regra de negócio, rota ou banco → também `./run-test.sh` (**avise antes: apaga o MySQL local**).
- Se falhar: mostre o resumo do script, explique a causa e corrija **só se for parte do trabalho atual** (ou se o PR não
  passa sem a correção, avisando). Nunca afrouxe teste.

## 4. Documentação afetada
Confira e atualize só o que a mudança tornou falso:

| Se mudou... | Atualize |
|---|---|
| rota, request ou response | `backend/README_CONTRATO_API.md` (+ avisar que `frontend/lib/api/types.ts` precisa acompanhar) |
| quantidade/classes de teste | tabela de testes em `backend/README.md` e `README.md` (`node scripts/gerar-svgs-readme.mjs` atualiza os números) |
| skills em `.claude/skills/` | tabela do `CLAUDE.md` e a seção "🤖 Skills de IA do time" do `README.md` |
| resolveu um débito | apague a linha em `planing/debitos-tecnicos.md` |
| concluiu item do roadmap ou do checklist | `planing/roadmap.md` / `planing/checklist-features.md` |
| decisão de arquitetura | `planing/decisoes.md` |

## 5. Commits semânticos
- Um assunto por commit: `build:` (pom, dependências), `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`, `ci:`. Escopo opcional: `test(repositories): ...`.
- Mensagem em português, no imperativo do time ("adiciona", "corrige", "atualiza").
- O hook `scripts/hooks/commit-msg` valida o padrão (se não estiver ativo, rode `scripts/instalar-hooks.sh`).
- Termine cada mensagem com a linha de coautoria definida pelo ambiente, quando houver.

## 6. Push e PR feature → develop
- Logo antes do push, repita o passo 2 (`git pull origin develop`): alguém pode ter mergeado enquanto você validava.
  Entrou algo novo? Rode a validação do passo 3 outra vez.
- `git push -u origin <branch>` só se a ordem da mensagem atual incluir push (ver o quadro 🔒 no topo).
- Descrição do PR no modelo do `CONTRIBUTING.md` ("O que foi feito", "Como testar", "Checklist"), com:
  - o resultado real da validação (ex.: `356 testes, 0 falhas`);
  - o que o revisor deve olhar com atenção (o Renan revê tudo na reunião semanal);
  - conflitos resolvidos no passo 2 e conflitos previsíveis com outras branches abertas.
- `gh pr create --base develop --head <branch> --title "<tipo>(escopo): ..." --body-file <arquivo>`.

## 7. CI e merge na develop
- Espere os checks: `gh pr checks <número> --watch`. Algum vermelho → **não mergeie**: leia o log
  (`gh run view <id> --log-failed | grep -E "ERROR|FAIL"`), corrija na mesma branch (novo commit, push) e espere de novo.
  Teste que falha só às vezes (depende da ordem ou de dado que sobrou de outro teste) também é bug: reproduza e corrija.
- PR só de docs ou só de frontend pode não ter check nenhum (o Backend CI só roda quando muda `backend/` ou `pom.xml`):
  aí vale a validação local do passo 3, e o resultado dela entra na descrição do PR.
- Verde → `gh pr merge <número> --merge --delete-branch` (merge commit; nada de squash, que perde a ancestralidade
  entre `develop` e `main`).
- `git switch develop` + `git pull origin develop`.

## 8. PR develop → main (aberto, sem merge)
- Já existe? `gh pr list --base main --head develop --state open`. Se existir, ele já recebeu o merge novo: atualize o
  título/descrição com `/notas-de-versao` se o conteúdo mudou e confira os checks dele (`gh pr checks <número>`).
- Não existe → `gh pr create --base main --head develop --title "release: ... (develop → main)" --body-file <arquivo>`,
  com as notas do `/notas-de-versao`.
- Check vermelho no PR `develop → main`? Corrija por uma branch nova a partir da `develop` (volte ao passo 1); nunca
  commite direto na `develop` nem na `main`.
- **Não mergeie.** O merge na `main` é do Kevin e do Renan, depois da reunião semanal.

## 9. Fechar
- Mostre: link do PR da feature, resultado do CI, commit do merge na `develop`, link do PR `develop → main` (aberto) e
  em que branch o checkout ficou (a `develop`).
- Mudou rota ou formato da API? Sugira o `/recado` para o Renan.
