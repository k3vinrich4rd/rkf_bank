---
name: entregar
description: Fecha uma entrega no RKF Bank - valida backend e/ou frontend com os scripts, confere a documentação afetada, organiza commits semânticos, faz push e escreve a descrição do Pull Request no modelo do CONTRIBUTING.md. Use quando o usuário disser "/entregar", "fecha o PR", "commita e sobe" ou similar.
---

# /entregar

Objetivo: levar o trabalho atual da branch até um PR pronto para revisão, gastando o mínimo de tokens.

## 1. Entender o que mudou
- `git status --short` e `git diff --stat origin/develop...HEAD` (mais o que não foi commitado).
- Classifique: mudou `backend/` ou `pom.xml`? mudou `frontend/`? só docs?
- Se a branch atual for `main` ou `develop`, **pare**: crie uma branch `feature/`, `fix/`, `docs/` ou `chore/` antes.

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
- `git push -u origin <branch>` (só se o usuário pediu push ou `/entregar` explicitamente).
- Escreva a descrição do PR no modelo do `CONTRIBUTING.md` ("O que foi feito", "Como testar", "Checklist"), com:
  - o resultado real da validação (ex.: `107 testes, 0 falhas`);
  - o que o revisor deve olhar com atenção;
  - conflitos previsíveis com outras branches abertas (ex.: as duas mexem no `<properties>` do `pom.xml`).
- Base do PR: `develop`. Entregue o link `https://github.com/k3vinrich4rd/rkf_bank/pull/new/<branch>`.
