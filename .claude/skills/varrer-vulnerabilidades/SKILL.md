---
name: varrer-vulnerabilidades
description: Procura CVEs conhecidas nas dependências do backend (Maven) e do frontend (pnpm) com scripts/varrer-cves.js e propõe a correção mínima segura, explicando o risco de cada uma. Use quando o usuário pedir "/varrer-vulnerabilidades", quando o GitHub avisar de vulnerabilidades, ou antes de um release.
---

# /varrer-vulnerabilidades

## 1. Varrer (script, não IA)
`node scripts/varrer-cves.js tudo` (ou `backend` / `frontend`). O script consulta o OSV.dev (base pública) e imprime pacote, versão, CVE, severidade e a versão que corrige.

## 2. Explicar
Para cada achado, em linguagem simples:
- **o que é a falha** (ex.: "bypass de autenticação", "DoS no parse de JSON") e se ela **afeta o RKF Bank de verdade** (a funcionalidade vulnerável é usada? ex.: autenticação DIGEST do Tomcat não é usada, mas o Tomcat responde a toda requisição);
- **de onde vem a dependência**: direta ou transitiva (`./mvnw dependency:tree -Dincludes=<groupId>:<artifactId>`).

## 3. Propor a correção mínima
Ordem de preferência:
1. Subir a versão do **Spring Boot** (parent do `pom.xml`) se a versão nova já traz a correção.
2. Se o Spring Boot mais novo ainda não trouxer: **sobrescrever a propriedade do BOM** (`<tomcat.version>`, `<jackson-bom.version>`...) com comentário dizendo quando remover, e registrar em `planing/debitos-tecnicos.md`.
3. Frontend: subir a versão no `package.json` com `pnpm` (versão do `packageManager`) e regenerar o `pnpm-lock.yaml`. **O frontend é do Renan**: só altere se o usuário pedir; senão, registre como débito técnico.
4. Sem correção publicada: registre o risco em `planing/debitos-tecnicos.md`.

## 4. Validar
- `scripts/validar.sh backend` / `frontend`.
- Para backend: subir a API com o MySQL e rodar `node scripts/smoke-e2e.mjs`, porque testes com mock não provam que o Tomcat/Jackson novos funcionam.
- Rodar a varredura de novo e mostrar que zerou.
- Commit: `fix(deps): ...` listando as CVEs no corpo.
