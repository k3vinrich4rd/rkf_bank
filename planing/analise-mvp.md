# Análise do MVP — o que falta até 31/10/2026

> ✅ **Encerrada em 29/10/2026:** o MVP foi concluído com a release do PR #76 na `main`, **sem deploy** (o projeto é de
> portfólio e roda localmente; decisão em [decisoes.md](decisoes.md)). O texto abaixo fica como histórico.

## ⏱️ Revisão de 26/10/2026 (5 dias para o prazo)

**O produto continua pronto; o lançamento continua parado nas mesmas decisões.** Desde 23/10 andou tudo o que era
código e teste, e **nada** do que era publicar.

| # | Item (tabela da seção 2) | 23/10 | 26/10 |
|:-:|---|:-:|---|
| 1 | Mergear o PR #60 (contrato 3.1) | ⏳ | ✅ na `develop` e na `main` (#68); CVEs do front (#67) e dependências do front (#69) também |
| 2 | Decidir onde publicar | ⏳ | 🟡 proposta em [`backend/README_DEPLOY.md`](../backend/README_DEPLOY.md): **A** grátis (Render + Aiven MySQL + Vercel) ou **B** sempre ligado (Railway ~US$ 5/mês + Vercel). Falta o time escolher |
| 3 | Perfil `prod` | ⏳ | ✅ `application-prod.properties` (sem seed, sem Swagger, segredos só do ambiente, CORS obrigatório) + `PerfilProdIntegracaoTest` (4 testes) |
| 4 | Empacotar API e site | ⏳ | ✅ API: `Dockerfile` (testado: 21/21 passos e smoke 42/42 num MySQL vazio, ~370 MB de 512 MB); imagem publicada no ghcr.io pela Action `backend-docker.yml`. Site: a Vercel publica o Next direto, sem imagem |
| 5 | Banco gerenciado com backup | ⏳ | ⏳ depende do item 2 |
| 6 | Encarregado LGPD | ⏳ | ✅ **Kevin e Renan**, também responsáveis pela publicação; seção 7 da política e termos `2026-10-26` |
| 7 | Teste do Fellipe | ⏳ | 🟡 **roteiros prontos e curtos** (`.equipe/testes/BACKEND_E2E.md`: ADMIN em 25 passos e cliente em 36, H2 ou Docker); dá para ele começar **hoje, local**, sem esperar o ambiente publicado |
| 10 | `develop` → `main` | — | PR #70 aberto para a reunião |

**Actions novas em 26/10:** `backend-docker.yml` (imagem no perfil `prod` + smoke) e `e2e.yml` (site + API + coleção, a aplicação inteira).

**Ajustes que a revisão de 26/10 achou para a publicação** (nenhum é bug; são do ambiente publicado):

| # | Ajuste | Quem | Por quê |
|:-:|---|---|---|
| a | ~~Encarregado LGPD na seção 7~~ ✅ feito em 26/10 (Kevin e Renan, termos `2026-10-26`). Falta só um e-mail dedicado, se o time quiser um canal privado além do GitHub | Kevin e Renan | hoje o contato é pelo GitHub de cada um |
| b | Tela de "acordando o servidor" no login e no cadastro, se for o caminho A (Render Free dorme) | Renan | a 1ª requisição depois de 15 min parada leva ~1 min; hoje vira o erro genérico de conexão |
| c | Deixar o pacote `rkfbank-api` do ghcr.io **público** depois do 1º push na `main` (ou dar ao provedor um token de leitura) | Kevin | o GitHub cria o pacote privado; o provedor não consegue puxar a imagem |
| d | `CORS_ORIGENS` e `API_URL` com os domínios definitivos | Kevin (API) e Renan (site) | os dois dependem um do outro: publicar a API primeiro, o site depois |
| e | Lembrete "use dados fictícios" também no cadastro (hoje só a landing avisa que é portfólio) | Renan | o banco publicado guarda o que a pessoa digitar |

**Coleções conferidas em 26/10** contra a API de verdade: `83/83` (visitante) e `119/119` (equipe).

**Sugestão para não perder o 31/10:** decidir o item 2 **hoje** pelo caminho simples (API no Render ou Railway com
MySQL gerenciado, site na Vercel); 3 e 4 feitos na `feature/deploy-mvp`; Fellipe testa local
em paralelo, e repete só a Parte 3 (cliente) do `BACKEND_E2E.md` no ambiente publicado. Se o 31/10 apertar, o corte honesto é lançar
**local + vídeo/prints** no README e publicar na semana seguinte: é melhor do que publicar sem perfil `prod` (o seed
da equipe e o Swagger iriam para a internet).

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
| 7 | **Teste do Fellipe** | Fellipe | sim | roteiro em `.equipe` (`BACKEND_E2E.md` / `FRONTEND_E2E.md`) contra o ambiente publicado. |
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
