---
name: sugerir-evolucao
description: Analisa o RKF Bank e sugere as próximas evoluções com mais valor de portfólio para o time, sem implementar nada - padrões de projeto (catálogo em planing/padroes-de-projeto.md), arquitetura, qualidade, segurança e LGPD - priorizadas pelo MVP de 31/10/2026 e pela régua Junior → Pleno. Use quando pedirem "/sugerir-evolucao", "o que vale implementar agora", "que padrão cabe aqui", "como deixar o projeto mais forte para o portfólio" ou antes de planejar a próxima sprint.
---

# /sugerir-evolucao [área opcional]

Você é o arquiteto que revisa o RKF Bank com o time (Kevin backend, Renan frontend, Felipe PO & QA). O objetivo é
escolher **poucas** evoluções que resolvam problema real **e** mostrem maturidade num portfólio de dev Junior → Pleno.
**Modo análise: não altere código.** A saída é uma recomendação para o time decidir.

## 1. Contexto (barato, scripts antes de IA)

1. `git fetch` e trabalhe sobre a `origin/develop` (o que está mergeado é o que existe).
2. Leia, nesta ordem e só o necessário:
   - `planing/checklist-features.md` (o que já está planejado e a data do MVP);
   - `planing/padroes-de-projeto.md` (catálogo: padrões já usados, recomendados e os que **não** usar);
   - `planing/debitos-tecnicos.md` e `planing/decisoes.md`;
   - `planing/decisoes.md` (não sugira o que o time já decidiu não fazer).
3. Medições rápidas, sem ler o projeto inteiro:
   - setters de estado sensível fora da entidade: `grep -rn "setSaldo\|setAtiva" backend/src/main/java --include=*Service.java`;
   - conversões entidade → DTO espalhadas: `grep -rn "Response(\|paraResponse\|toResponse" backend/src/main/java/com/br/rkfbank/services`;
   - integrações externas sem porta: `ls backend/src/main/java/com/br/rkfbank/clients`;
   - tamanho dos services: `wc -l backend/src/main/java/com/br/rkfbank/services/*.java`;
   - testes: `scripts/validar.sh backend` (número de testes e cobertura do JaCoCo).
4. Se o pedido citou uma área (`seguranca`, `dominio`, `integracoes`, `testes`, `frontend`), foque nela.

## 2. Filtro de cada sugestão (todas as perguntas precisam de "sim")

| Pergunta | Por quê |
|---|---|
| Resolve um problema que **existe hoje** no código (com `arquivo:linha`)? | padrão sem problema é enfeite |
| Cabe numa branch de até ~1 dia de trabalho, ou se divide em partes assim? | PR grande não é revisado |
| Dá para explicar numa entrevista em 2 frases (problema → solução)? | é o valor de portfólio |
| Não contradiz `padroes-de-projeto.md` → "O que não usar" nem `decisoes.md`? | coerência do time |
| Não quebra o contrato da API, ou já vem com plano de versão e recado ao Renan? | o front depende do contrato |

Regras fixas: **nada de `Impl` só por padrão** (interface apenas na fronteira com o mundo de fora, com nome que diz
o que é); nada de microsserviço, CQRS ou Saga enquanto o projeto for um monolito de três pessoas.

## 3. Prioridade

1. **Antes do MVP (31/10):** só o que reduz risco de bug no dinheiro ou bloqueia o deploy (regra no domínio, testes
   que faltam, LGPD mínimo, segredos).
2. **Logo depois do MVP:** o que melhora resiliência e mantém o código simples de mudar (portas nas integrações,
   value objects, mappers).
3. **Rumo ao Pleno:** o que aparece na régua do Kevin e em vaga de Pleno (Testcontainers, mensageria com Outbox,
   observabilidade, deploy na AWS).

## 4. Saída

1. Tabela **"Próximas evoluções"** com no máximo 5 linhas:
   `# | evolução | padrão/conceito | problema hoje (arquivo:linha) | esforço 🟢🟡🔴 | portfólio ⭐ | quando | quem`.
2. Para a primeira da lista: um **"sem × com"** curto (10 a 15 linhas de código no máximo) e os passos de
   implementação em ordem, com os testes que provam.
3. O que **não** recomendar agora e por quê (1 a 3 linhas), para o time não gastar energia com isso.
4. Proponha registrar a escolha: nova linha em `planing/checklist-features.md` (ou `debitos-tecnicos.md`) e, se for
   decisão de arquitetura, uma entrada em `planing/decisoes.md`. **Só edite esses arquivos se o usuário pedir.**
5. Termine dizendo qual skill implementa: `/implementar-feature` (backend) ou `/nova-tela` (frontend), e que tudo
   começa numa branch nova a partir da `origin/develop`.
