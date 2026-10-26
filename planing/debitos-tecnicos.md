# Débitos técnicos

Débito técnico é algo que funciona hoje, mas que vai dar trabalho ou problema no futuro.
Anotar aqui não é reclamar do código: é não esquecer.

**Prioridade:** 🔴 resolver antes do MVP · 🟡 logo depois do MVP · 🟢 quando der · 🔮 futuro (ainda sem plano)

Resolveu um item? Apague a linha no mesmo PR.

---

## Backend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🔴 | **LGPD:** a política de privacidade, os termos e o aceite versionado já existem (contrato 3.1), mas o **encarregado** (quem responde pelos dados pessoais) e o canal oficial de contato ainda não foram definidos. A página de privacidade avisa que o canal "será publicado antes do lançamento". | Decidir na reunião quem é o encarregado e qual e-mail atende os titulares; publicar na seção 7 de `frontend/app/privacidade/page.tsx` e subir a versão dos termos (`rkfbank.lgpd.termos-versao`). |
| 🟢 | O `pom.xml` fica na raiz, mas o código fica em `backend/`. | Mover `pom.xml` e `mvnw` para `backend/` numa semana sem PR aberto: muda o CI (`backend-ci.yml`, CodeQL), o Dependabot, os scripts (`run-test.sh`, `validar.sh`, `testar-no-mysql.sh`), as configurações `.run/` do IntelliJ e o caminho do `.env`. |

## Frontend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟢 | O seletor de conta já troca entre as contas do cliente, mas a tela de abertura só abre a **primeira** conta: quem já tem conta vê "você já tem conta" e não consegue abrir a poupança pela interface (a API aceita, contrato 4.1). | Decidir com o time se o produto oferece a segunda conta; se sim, liberar a abertura do outro tipo em `components/abrir-conta/use-abrir-conta.ts`. |
| 🟢 | `braces` 3.0.3 ([alerta #7 do Dependabot](https://github.com/k3vinrich4rd/rkf_bank/security/dependabot/7), CVE-2026-93687, **alta**: DoS por estouro de pilha com padrões muito aninhados) **ainda sem correção publicada**. Chega por `micromatch` ← `fast-glob`, usados pelo `eslint-config-next` e pelo CLI `shadcn` (`ts-morph`). Fica em 🟢 apesar da gravidade porque só roda no lint e nas ferramentas da máquina de quem desenvolve: não vai para o build do site e nunca recebe entrada de usuário. Revisado em 23/10/2026: o `braces` continua em 3.0.3 no npm; o OSV passou a registrar a CVE pelo repositório Git, então `scripts/varrer-cves.js` (que consulta por versão do npm) não a mostra mais, mas ela **não foi corrigida**. | Conferir `npm view braces version` de vez em quando e atualizar quando sair a correção (o Dependabot do `frontend/` também avisa), ou quando `eslint-config-next`/`shadcn` trocarem de dependência. Área do Renan. |

## Projeto

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟡 | **Não existe perfil `prod` nem `Dockerfile`** (itens 3 e 4 de [analise-mvp.md](analise-mvp.md)): o perfil padrão carrega o seed da equipe e deixa o Swagger ligado. Bloqueia publicar, não o código. | `application-prod.properties` (Flyway só com `db/migration`, Swagger desligado, `CORS_ORIGENS` do domínio) e um `Dockerfile` multi-stage com o `.jar` (Java 25), assim que o time decidir onde publicar. |
| 🟢 | O guia de testes do frontend (`frontend/README_TESTES_E2E.md`) ainda é só o roteiro longo; a API ganhou a trilha curta ([VISITANTES.md](../VISITANTES.md), 13 passos). | Trilha curta das telas no mesmo formato (onde clicar, o que aparece, caixinha). Área do Renan. |
| 🟢 | Recados antigos ficam abertos (`- [ ]`) depois de resolvidos, e a lista de [recados.md](recados.md) fica difícil de ler. | Quem recebe marca `[x]` com uma linha de resposta no mesmo PR que resolve. |
| 🟢 | A pasta `planing` está escrita errado (o certo é `planning`). | Renomear quando ninguém estiver mexendo nela: além dos links do repositório, mudam as skills em `.claude/skills` e os roteiros do `.equipe` que apontam para cá. |

---

## Funcionalidades futuras 🔮

Funcionalidades de banco que o RKF Bank ainda não tem. **Não existe plano de implementação**: cada item lista
o que precisa existir antes e as perguntas que o time precisa responder. Quando um item virar prioridade, ele
ganha uma seção completa em [checklist-features.md](checklist-features.md), no mesmo formato das features 1 a 4.

> ⚠️ Pix e boleto de verdade exigem autorização do Banco Central e integração com a rede bancária (um PSP ou um
> banco parceiro). No RKF Bank, a ideia é **simular** as regras entre as contas do próprio banco, para aprender
> o domínio sem depender de terceiros.

### Pagamentos

| Prioridade | Funcionalidade | Precisa existir antes | Perguntas para responder |
|:---:|---|---|---|
| 🔮 | **Pix**: cadastrar chaves (CPF, e-mail, telefone, aleatória) e transferir pela chave | login (Spring Security), Flyway | Uma chave pode estar em duas contas? Como funciona a portabilidade de chave? Qual o limite por transação e por período noturno? Como mostrar o nome do recebedor antes de confirmar? |
| 🔮 | **Pix cobrança (copia e cola)**: gerar um código para receber um valor | Pix | Como é o formato BR Code (padrão EMV) e o CRC16 que valida o código? O código tem validade? Valor fixo ou aberto? |
| 🔮 | **Boleto: gerar** um boleto para receber (linha digitável, código de barras e vencimento) | login, Flyway | Como se monta a linha digitável e os dígitos verificadores (módulo 10 e 11)? O que acontece depois do vencimento (multa e juros)? |
| 🔮 | **Boleto: pagar** um boleto, debitando a conta | gerar boleto, login | Como validar a linha digitável antes de debitar? Pagamento agendado para o vencimento? O que fazer com um boleto já pago? |
| 🔮 | **Leitura de QR Code e código de barras**, para casos em que digitar é inviável: Pix copia e cola e boleto | Pix cobrança e pagamento de boleto | Ler pela câmera no frontend ou por upload de imagem? A leitura acontece no navegador e só o texto vai para a API? Como tratar câmera negada, QR inválido ou código adulterado? |
| 🔮 | **Comprovante** de Pix, boleto e transferência (tela e PDF) | Pix ou boleto | Quais dados aparecem mascarados? Gerar no backend ou no frontend? |

### Ideias para o futuro (insights)

| Prioridade | Ideia | Por que é interessante | Precisa existir antes |
|:---:|---|---|---|
| 🔮 | **Limites e antifraude simples**: limite diário, limite noturno, bloqueio após várias tentativas | regra de negócio real de banco; ótimo para treinar testes de regra | login |
| 🔮 | **Transferência agendada e recorrente** | aprender jobs agendados (`@Scheduled`) e idempotência | Flyway |
| 🔮 | **Rendimento da poupança** calculado todo dia | o tipo `POUPANCA` já existe e hoje não rende nada; treina cálculo com `BigDecimal` e jobs | Flyway |
| 🔮 | **Extrato com filtro e exportação** (período, tipo; CSV ou PDF) | consultas paginadas com filtro e geração de arquivo | `GET /contas/{contaId}` |
| 🔮 | **Notificações** (e-mail) a cada movimentação | primeiro caso de uso concreto para a mensageria | mensageria (feature 6) |
| 🔮 | **Autenticação em dois fatores** (código por e-mail ou app autenticador) | segurança de nível bancário | login |
| 🔮 | **Auditoria**: quem fez o quê, quando e de onde | rastreabilidade além dos logs; exigência comum em banco | login, Flyway |
| 🔮 | **Encerrar e reativar conta** | o campo `ativa` já existe, mas não há rota para mudar | login |
| 🔮 | **Cartão virtual** (número, validade e CVV gerados, com bloqueio) | domínio novo com regras próprias (algoritmo de Luhn) | login |
| 🔮 | **Painel de gastos** por categoria no frontend | dá visibilidade ao extrato e treina gráficos no frontend | extrato com filtro |
