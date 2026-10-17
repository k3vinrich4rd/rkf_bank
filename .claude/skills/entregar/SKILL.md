---
name: entregar
description: Fecha uma entrega no RKF Bank - valida backend e/ou frontend com os scripts, confere a documentação afetada, organiza commits semânticos, faz push e escreve a descrição do Pull Request no modelo do CONTRIBUTING.md. Use quando o usuário disser "/entregar", "fecha o PR", "commita e sobe" ou similar.
---

# /entregar

Objetivo: levar o trabalho atual da branch até um PR pronto para revisão, gastando o mínimo de tokens.

> 🔒 **Esta é a única skill que commita e faz push, e só com ordem explícita do Kevin na mensagem atual.**
> - `/entregar` digitado por ele, ou "commita e sobe" → pode commitar **e** fazer push.
> - "commita" sem falar em push → só commita.
> - Chegou aqui por sugestão de outra skill, ou o pedido é ambíguo → **pare** antes do passo 4, mostre o plano de commits e pergunte.
> - Nunca `reset`, `revert`, `rebase` ou `push --force` sem pedido explícito.

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

## 5. Push e PR
- `git push -u origin <branch>` só se a ordem da mensagem atual incluir push (ver o quadro 🔒 no topo).
- Escreva a descrição do PR no modelo do `CONTRIBUTING.md` ("O que foi feito", "Como testar", "Checklist"), com:
  - o resultado real da validação (ex.: `107 testes, 0 falhas`);
  - o que o revisor deve olhar com atenção;
  - conflitos previsíveis com outras branches abertas (ex.: as duas mexem no `<properties>` do `pom.xml`).
- Base do PR: `develop`. Entregue o link `https://github.com/k3vinrich4rd/rkf_bank/pull/new/<branch>`.
