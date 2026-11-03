# 🚀 Publicar o RKF Bank (perfil `prod`)

> 🚀 **Pronto para publicar** (preparo de 03/11/2026): banco no **Aiven** (MySQL com SSL), API no **Render**
> (Docker, `render.yaml` na raiz) e site na **Vercel**. As URLs abaixo ficam como espaço reservado até a publicação:
>
> | | Endereço |
> |---|---|
> | 🌐 Site | `<URL_DO_FRONT_NA_VERCEL>` |
> | ⚙️ API | `<URL_DO_BACKEND_NO_RENDER>` |

| | |
|---|---|
| 🎯 **Para que serve** | colocar a API e o site na internet: onde publicar, a imagem Docker, as variáveis e o que conferir depois |
| 👥 **Área responsável** | **Kevin e Renan** são os responsáveis pela publicação: 🟦 Kevin pela API, 🟩 Renan pelo site |
| ⏰ **Quando usar** | a primeira publicação do MVP e cada nova versão que sair da `main` |
| 🚫 **Não cobre** | testar o `prod` na sua máquina antes de publicar: roteiro privado da equipe (`.equipe/testes/PROD_E2E.md`) |

## 1. Como o RKF Bank fica publicado

```text
navegador ──HTTPS──▶ site Next.js (Vercel) ──/api/* BFF, prazo 12 s──▶ API Docker, perfil prod (Render) ──SSL REQUIRED──▶ MySQL (Aiven)
```

O navegador só fala com o site. O site repassa `/api/*` para a API pelo servidor dele (`API_URL`), então o endereço
da API nunca aparece no navegador e o token fica num cookie `httpOnly`. Por que cada peça é assim:

| Decisão | Por quê |
|---|---|
| SSL `REQUIRED` no banco | o Aiven fica fora da rede do Render, na internet pública: sem SSL obrigatório, senha e dados poderiam trafegar sem criptografia (seção 2.2) |
| Datas em UTC (`hibernate.jdbc.time_zone=UTC`) | o horário gravado não muda com o fuso da JVM nem com o provedor (seção 4) |
| Prazo no BFF + aviso de "servidor acordando" | a API hiberna no Render Free e leva ~1 min para voltar: a tela explica em vez de girar (seção 5.1) |

## 2. Onde publicar (caminho escolhido: A · grátis)

Os preços e limites abaixo foram consultados em 26/10/2026 e mudam com frequência: confira a página do provedor
antes de decidir.

| Caminho | API | Banco | Site | Custo | Bom para | Cuidado |
|---|---|---|---|:-:|---|---|
| **A · Grátis** ✅ | Render, plano Free (Docker, 512 MB) | Aiven for MySQL, plano Free (1 GB) | Vercel Hobby | **R$ 0** | publicar sem cartão e mostrar no portfólio | a API **dorme** após 15 min sem uso e leva ~1 min para acordar; o Render avisa que o Free não é para produção |
| **B · Sempre ligado** | Railway, plano Hobby | MySQL do próprio Railway (mesmo projeto, rede interna) | Vercel Hobby | ~US$ 5/mês | demo que responde na hora, API e banco no mesmo painel | cobrança por uso: configure o alerta de gasto |
| **C · AWS** | App Runner ou ECS | RDS MySQL | Amplify ou Vercel | variável | aprender cloud (feature 5 do checklist) | precisa do estudo antes |

A imagem é a mesma nos três: mudar de casa é trocar as variáveis. Medido em 26/10, a API no perfil `prod` usa
**~370 MB** dos 512 MB do container, por isso cabe no plano grátis.

> ⚠️ **Vercel Hobby é só para uso pessoal e não comercial.** Um banco de portfólio, com dinheiro de mentira, se
> encaixa. Se o RKF Bank virar produto (com receita), o site vai para o plano pago ou para outro provedor.

### 2.1 Passo a passo (na ordem)

| # | Onde | O que fazer | Pronto quando |
|:-:|---|---|---|
| 1 | **Aiven** | criar um serviço *MySQL* no plano Free; anotar host, porta, banco (`defaultdb` ou um criado para o projeto), usuário e senha da aba *Overview* | o serviço aparece como *Running* |
| 2 | **Render** | *New → Blueprint*, escolher este repositório: o Render lê o [`render.yaml`](../render.yaml) e cria o Web Service `rkfbank-api` (Docker, plano free, health em `/actuator/health`) | o painel pede os valores das variáveis `sync: false` |
| 3 | **Render** | preencher as variáveis da seção 4 (ainda sem `CORS_ORIGENS` definitivo, se o site não existir: use um valor provisório e troque no passo 5) | deploy *Live* e `<URL_DO_BACKEND_NO_RENDER>/actuator/health` → `UP` |
| 4 | **Vercel** | *Add New → Project*, este repositório, **Root Directory `frontend`**, variável `API_URL=<URL_DO_BACKEND_NO_RENDER>` (seção 5) | o site abre em `<URL_DO_FRONT_NA_VERCEL>` |
| 5 | **Render** | trocar `CORS_ORIGENS` para `<URL_DO_FRONT_NA_VERCEL>` e salvar (o Render reinicia a API) | preflight de outra origem → `403` |
| 6 | todos | rodar o checklist da seção 6 | todos os itens ✅ |

### 2.2 Aiven: banco com SSL

- O Aiven **exige SSL** em toda conexão e o banco fica na internet pública, fora da rede do Render.
- A URL JDBC do perfil `prod` usa `sslMode=${DB_SSL_MODE:REQUIRED}` (parâmetro do MySQL Connector/J; os valores
  válidos são `DISABLED`, `PREFERRED`, `REQUIRED`, `VERIFY_CA` e `VERIFY_IDENTITY`).
- **Por que `REQUIRED` é o padrão (antes era `PREFERRED`):** com `PREFERRED` o driver aceita cair para uma conexão
  **sem** criptografia se o SSL falhar, e a senha do banco e os dados dos clientes passariam em texto puro pela
  internet. `REQUIRED` recusa conectar sem SSL: melhor a API não subir do que subir exposta.
- `REQUIRED` criptografa mas **não confere o certificado** do servidor. O próximo passo, se quiser fechar também o
  risco de alguém se passar pelo banco, é `VERIFY_CA` com o certificado CA do Aiven (aba *Overview → CA certificate*)
  num truststore dentro da imagem (`trustCertificateKeyStoreUrl`). Fica como evolução: exige mudar o `Dockerfile`.
- As tabelas do projeto têm chave primária (o Aiven liga `sql_require_primary_key`), e o Flyway cria o schema na
  primeira subida, com o banco vazio.

### 2.3 Render: a API em Docker

- O [`render.yaml`](../render.yaml) (Blueprint, [formato oficial](https://render.com/docs/blueprint-spec)) é
  **opcional**: dá para criar o Web Service à mão com as mesmas opções. Ele define `runtime: docker`, `plan: free`,
  `dockerfilePath: ./Dockerfile` (o Dockerfile fica na raiz, junto do `pom.xml`), `healthCheckPath: /actuator/health`
  e um `buildFilter` para mudanças só no `frontend/` não reconstruírem a API.
- `SPRING_PROFILES_ACTIVE=prod` vem com valor (não é segredo). Todas as outras variáveis estão com `sync: false` e
  **sem valor**: o Render pede no painel na criação do Blueprint. Nas sincronizações seguintes o Render **ignora**
  `sync: false`; variável nova depois disso entra à mão no painel.
- O Render define `PORT` (a API lê `server.port=${PORT:8080}`) e termina o HTTPS (a API usa os `X-Forwarded-*`).
- **Hibernação do plano Free:** sem tráfego por 15 min, o Render desliga a API; a próxima requisição espera ~1 min
  (subida da JVM + Flyway). O site trata isso (seção 5.1) em vez de ficar girando.
- O health check só libera a versão nova quando ela responde `UP`: deploy com variável faltando falha sem derrubar a
  versão que estava no ar.

## 3. A imagem da API

```bash
docker build -t rkfbank-api .                                   # na raiz: o Dockerfile está aqui
docker run -p 8080:8080 --env-file deploy.env rkfbank-api       # deploy.env fica FORA do Git (.gitignore)
```

O GitHub Actions (`backend-docker.yml`) já constrói a imagem, sobe num container de 512 MB com um MySQL **vazio**,
confere o perfil `prod` e roda o smoke ponta a ponta. A cada push na `main`, ele também publica a imagem em
`ghcr.io/k3vinrich4rd/rkfbank-api` (`:latest` e `:<commit>`): o provedor pode puxar a imagem pronta ou construir
pelo `Dockerfile` do repositório.

## 4. Variáveis da API (painel do provedor)

| Variável | Obrigatória | Exemplo / formato | Para quê |
|---|:-:|---|---|
| `SPRING_PROFILES_ACTIVE` | | `prod` (já no `Dockerfile` e no `render.yaml`) | liga o perfil de produção |
| `DB_HOST` | ✅ | `<HOST_DO_MYSQL_NO_AIVEN>` | endereço do MySQL (painel do Aiven) |
| `DB_PORT` | ✅ no Aiven | `<PORTA_DO_MYSQL_NO_AIVEN>` | a do Aiven não é a `3306` padrão; veja no painel |
| `DB_NAME` | ✅ | `<NOME_DO_BANCO>` | nome do banco (`defaultdb` no Aiven, ou um criado para o projeto) |
| `DB_USER` / `DB_PASSWORD` | ✅ | do painel do Aiven | login do banco |
| `DB_SSL_MODE` | | `REQUIRED` (padrão) · `VERIFY_CA` · `VERIFY_IDENTITY` | SSL obrigatório (seção 2.2). No Render, **preencha** `REQUIRED`: variável vazia não cai no padrão |
| `JWT_SEGREDO` | ✅ | gere com `openssl rand -base64 48` | assina os tokens; **nunca** o mesmo do `.env` de alguém |
| `CORS_ORIGENS` | ✅ | `<URL_DO_FRONT_NA_VERCEL>` | domínio do site (sem barra no fim) |
| `ADMIN_CPF` / `ADMIN_SENHA` | | CPF válido e senha forte | cria o primeiro ADMIN na primeira subida |
| `JWT_EXPIRACAO` | | `15m` (padrão) | validade do token de acesso |
| `PORT` | | o provedor define | porta HTTP |

Sem as obrigatórias, a API **não sobe** (de propósito: melhor parar do que rodar com segredo errado). Não
existem `EQUIPE_SENHA` nem dados de teste em produção: o banco nasce vazio, só com o schema do Flyway. Nenhum valor
real vai para o Git: só o painel do provedor conhece os segredos.

**Datas em UTC:** o perfil `prod` liga `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` (além do
`serverTimezone=UTC` da URL). Por quê: o `Dockerfile` usa `TZ=America/Sao_Paulo` e cada provedor pode usar outro
fuso; sem isso o Hibernate grava os `Instant` (lançamentos, tentativas de login, tokens de renovação) no fuso da
JVM, e trocar de provedor ou de TZ deslocaria o extrato e as validades em horas. Com UTC no banco, o horário
gravado não depende de onde a API roda; a tela converte para o fuso de quem vê.

Health check do provedor: `GET /actuator/health` (público, responde só `UP`/`DOWN`).

## 5. O site (Vercel)

| Configuração | Valor |
|---|---|
| Root Directory | `frontend` |
| Framework | Next.js (detectado sozinho) |
| Variável `API_URL` | `<URL_DO_BACKEND_NO_RENDER>` (sem barra no fim). Lida só no servidor do Next: não vai para o navegador |
| Variável `NEXT_PUBLIC_LOG_LEVEL` | `warn` (opcional) |

Em produção, o cookie de sessão sai com `secure` (só HTTPS), o que a Vercel já garante.

### 5.1 Timeout no BFF e o aviso de "servidor acordando"

Toda chamada do site à API passa pelo servidor do Next (BFF): `app/api/[...caminho]` (que substituiu o antigo
rewrite do `next.config.ts`), as rotas de `app/api/auth/*` e a renovação de sessão do `proxy.ts`. Todas usam
`frontend/lib/api/backend.ts`:

| Regra | Valor | Por quê |
|---|---|---|
| Prazo por tentativa (AbortController) | **12 s** | uma resposta normal leva bem menos; sem prazo, com a API hibernando a tela ficaria ~1 min girando sem explicação |
| Nova tentativa | 1, depois de **2 s**, só em GET/HEAD e no login | repetir é seguro só onde não há efeito colateral; um depósito que estourou o prazo pode ter chegado à API |
| Sem nova tentativa | POST/PATCH/DELETE, troca de senha, renovação | a renovação usa token de uso único: repetir pareceria reuso e a API derrubaria todas as sessões |
| API sem resposta (prazo, conexão recusada, 502/503/504 do provedor) | 503 `SERVICO_INDISPONIVEL` + `Retry-After: 30` | a tela mostra "O servidor está acordando... leva cerca de 1 minuto" em vez de um erro genérico |
| Renovação sem resposta | a sessão **fica** | hibernação não é sessão expirada: quando a API acordar, a mesma renovação vale |

O 503 da própria API (ViaCEP fora do ar) vem em JSON e passa como veio, com a mensagem dele.

## 6. Depois de publicar, confira

1. `<URL_DO_BACKEND_NO_RENDER>/actuator/health` → `UP` (a primeira chamada depois de hibernar leva ~1 min).
2. `<URL_DO_BACKEND_NO_RENDER>/swagger-ui.html` → `404` (fechado em produção).
3. Log da API no Render: `profile is active: "prod"` e o Flyway aplicando as migrações no banco vazio (prova de que
   a conexão com SSL `REQUIRED` funcionou).
4. Preflight de origem estranha → `403`:
   `curl -s -o /dev/null -w '%{http_code}' -X OPTIONS <URL_DO_BACKEND_NO_RENDER>/contas -H 'Origin: https://site-estranho.example' -H 'Access-Control-Request-Method: GET'`
5. No site (`<URL_DO_FRONT_NA_VERCEL>`): cadastro (o CEP vira endereço pelo ViaCEP), login, abrir conta, depósito,
   saque e extrato. Com a API hibernando, a tela mostra o aviso de "servidor acordando" e funciona depois de ~1 min.
6. `node scripts/smoke-e2e.mjs <URL_DO_BACKEND_NO_RENDER>`. ⚠️ Ele cria clientes de teste no banco de produção:
   rode antes de abrir para o público, ou não rode lá.
7. Primeiro login do ADMIN funcionando; depois, troque a senha dele (`PATCH /usuarios/me/senha`).
8. Troque os espaços reservados `<URL_DO_BACKEND_NO_RENDER>` e `<URL_DO_FRONT_NA_VERCEL>` deste guia e do README pelas URLs reais.

## 7. Antes de abrir para o público

- **LGPD:** os encarregados (Kevin e Renan) já estão na seção 7 da política (termos `2026-10-26`). Mudou o texto?
  Suba `TERMOS_VERSAO` (frontend) **e** `LGPD_TERMOS_VERSAO` (variável da API) juntos.
- Deixe claro na tela que é um **banco de demonstração** (dinheiro fictício), para ninguém digitar dado real além do necessário.
- Alerta de custo ligado no provedor (caminho B) e backup do banco (o Aiven Free não promete backup de produção).
