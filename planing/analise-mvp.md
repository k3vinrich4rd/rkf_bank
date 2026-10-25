# Análise do MVP — o que falta até 31/10/2026

Revisão do projeto inteiro em **23/10/2026** (8 dias para o prazo), comparando o que o [roadmap](roadmap.md), o
[checklist](checklist-features.md) e os [débitos](debitos-tecnicos.md) dizem com o que o código faz hoje
(branch `refactor/contrato-3-0-integracao`, PR #60).

**Resumo:** o produto do MVP está pronto e testado (backend 307 testes, 7 no MySQL real; frontend com lint, tipos,
31 testes e build). O que separa o RKF Bank do lançamento **não é código de funcionalidade**: é publicar
(decisão de onde + perfil de produção), definir o encarregado da LGPD e o teste do Fellipe.

---

## 1. O MVP mapeado está certo?

O escopo ("cadastro com CEP, abrir conta, depositar, sacar, transferir, ver saldo e extrato") continua certo e
**está todo implementado** nas duas pontas. O mapeamento em volta dele tinha seis desvios:

| # | O que o planejamento dizia | O que é verdade hoje | Ajuste |
|:-:|---|---|---|
| 1 | Roadmap, semana 2: falta `GET /clientes/{clienteId}/contas` | Rota **obsoleta** desde o contrato 2.6: o cliente vem do token, e `GET /contas` já lista as contas de quem está logado. Criar a rota por id de cliente reabriria a porta para ver conta dos outros. | Riscada do roadmap como substituída por `GET /contas`. |
| 2 | Roadmap: falta `GET /contas/{contaId}` | Feito (contrato 3.1), com regra de dono (`403` na conta de outro cliente). | Marcado. |
| 3 | "Colocar no ar" na semana 4, mas a feature 5 (AWS) é 🔮 futuro | Contradição: o lançamento depende de um lugar para publicar, e a única feature de deploy está marcada como "não sabemos fazer". | Separar **publicar o MVP de um jeito simples** (decisão desta semana, seção 3) da **AWS** (estudo futuro, continua 🔮). |
| 4 | Testcontainers e Swagger listados em "Depois do MVP" | Os dois prontos (features 3 e 4 ✅). | Saíram de "Depois do MVP". |
| 5 | "Login" em "Depois do MVP" | Login está no MVP desde os PRs #24 e #27, com *refresh token* agora. | Linha removida. |
| 6 | LGPD mínimo pendente | Termos, política, aceite versionado (`/termos`, V10), CPF mascarado e anonimização feitos. **Falta só o encarregado.** | Débito 🔴 reescrito só com o encarregado. |

Também não há nada de funcionalidade faltando para o MVP: Pix, boleto e cartão continuam 🔮 e **não** devem
entrar antes do lançamento.

## 2. O que falta, em ordem

| Ordem | Item | Quem | Bloqueia o lançamento? | Detalhe |
|:-:|---|---|:-:|---|
| 1 | **Revisar e mergear o PR #60** na `develop` | Kevin + Renan | sim | contrato 3.1 inteiro: refresh token, aceite LGPD, senha temporária, conciliação, MockMvc, Testcontainers, Swagger, seletor de conta, Vitest e CI do front. Recado em [recados.md](recados.md). |
| 2 | **Decidir onde publicar** | time | sim | seção 3. É a única decisão que trava o resto. |
| 3 | **Perfil `prod`** no backend | Kevin | sim | hoje não existe: o perfil padrão carrega o seed da equipe (`db/seed/equipe`) a menos que `FLYWAY_LOCATIONS` mude, e o Swagger fica ligado. O `prod` precisa de: `FLYWAY_LOCATIONS=classpath:db/migration` (sem dados de teste), `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false`, `CORS_ORIGENS` com o domínio do site, `JWT_SEGREDO` forte e `ADMIN_*` vindos do provedor, nunca do repositório. |
| 4 | **Empacotar** API e site | Kevin (API) · Renan (site) | sim | não há `Dockerfile` ainda. API: imagem com o `.jar` (Java 25). Site: depende do provedor (Vercel publica o Next direto; outros pedem imagem com `next start`). |
| 5 | **Banco gerenciado** com backup | Kevin | sim | MySQL 8.4; as migrações V1 a V15 já provaram rodar no MySQL real (`MigracoesMySqlTest`). |
| 6 | **Encarregado LGPD** e canal de contato | time | sim | débito 🔴: publicar na seção 7 da política e subir a versão dos termos. |
| 7 | **Teste do Fellipe** | Fellipe | sim | roteiro em `.equipe` (FRONTEND_E2E / BACKEND_E2E) contra o ambiente publicado. |
| 8 | Corrigir o que o Fellipe achar | Kevin + Renan | sim | — |
| 9 | README com link e prints | Kevin | não | último passo do roadmap. |
| 10 | Reunião aprova `develop` → `main` (PR #51) | Kevin + Renan | — | a `main` só recebe o que for aprovado na reunião semanal. |

Fica para depois do MVP (já anotado em [debitos-tecnicos.md](debitos-tecnicos.md)): mover o `pom.xml` para
`backend/`, renomear `planing`, abrir a segunda conta pela interface e acompanhar o `braces`.

## 3. Decisões que AWS e Mensageria esperam

### Publicar o MVP (decidir esta semana)

1. **Onde:** um serviço simples (Render, Railway, Fly.io; site na Vercel) ou já a AWS? O simples sobe em horas e
   tem plano gratuito ou barato; a AWS ensina mais, mas pede o estudo da feature 5 antes.
2. **Quanto podemos gastar por mês** e quem paga: decide se o banco é gerenciado (com backup) ou um container.
3. **Em nome de quem fica a conta** do provedor e quem tem acesso de administrador.
4. **Onde ficam os segredos** (`JWT_SEGREDO`, senha do banco, `ADMIN_*`): no painel do provedor; nunca no Git.
5. **Domínio:** subdomínio do provedor ou domínio próprio (afeta o `CORS_ORIGENS` e o cookie `secure`).
6. **Quem cuida do ambiente** depois que ele estiver no ar (alerta de custo, backup, atualização).

### Feature 5 — AWS (continua 🔮)

As mesmas perguntas, mais: qual serviço roda a API (Elastic Beanstalk, ECS/Fargate, App Runner ou EC2), RDS para
o MySQL, como o site chega (Amplify, S3 + CloudFront ou continua na Vercel), alerta de custo no Billing e se vale
estudar para a Cloud Practitioner antes. Ela pode virar a **segunda** casa do projeto, migrando do serviço simples.

### Feature 6 — Mensageria (continua 🔮)

1. **Precisa mesmo?** Hoje nada exige processamento assíncrono; a justificativa é aprender.
2. **Primeiro caso de uso:** comprovante, notificação por e-mail ou Pix (este último ainda nem existe).
3. **Evento interno do Spring** (`@TransactionalEventListener`, sem infraestrutura) **ou fila de verdade**?
4. Se fila: **RabbitMQ ou Kafka**, e onde ela roda no ambiente publicado (mais custo).
5. O que fazer com mensagem **perdida** ou **duplicada** (a idempotência que já temos ajuda aqui).

Sugestão: começar por evento interno do Spring com a notificação de movimentação, e só trocar por fila quando
houver um segundo serviço consumindo.
