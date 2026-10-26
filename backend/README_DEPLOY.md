# 🚀 Publicar o RKF Bank (perfil `prod`)

| | |
|---|---|
| 🎯 **Para que serve** | colocar a API e o site na internet: onde publicar, a imagem Docker, as variáveis e o que conferir depois |
| 👥 **Área responsável** | **Kevin e Renan** são os responsáveis pela publicação: 🟦 Kevin pela API, 🟩 Renan pelo site |
| ⏰ **Quando usar** | a primeira publicação do MVP e cada nova versão que sair da `main` |
| 🚫 **Não cobre** | testar o `prod` na sua máquina antes de publicar: roteiro privado da equipe (`.equipe/testes/PROD_E2E.md`) |

## 1. Como o RKF Bank fica publicado

```text
navegador ──HTTPS──▶ site (Next.js) ──/api/* (proxy do Next)──▶ API (imagem Docker, perfil prod) ──▶ MySQL gerenciado
```

O navegador só fala com o site. O site repassa `/api/*` para a API pelo servidor dele (`API_URL`), então o endereço
da API nunca aparece no navegador e o token fica num cookie `httpOnly`.

## 2. Onde publicar (proposta para o MVP de 31/10/2026)

Os preços e limites abaixo foram consultados em 26/10/2026 e mudam com frequência: confira a página do provedor
antes de decidir.

| Caminho | API | Banco | Site | Custo | Bom para | Cuidado |
|---|---|---|---|:-:|---|---|
| **A · Grátis** | Render, plano Free (Docker, 512 MB) | Aiven for MySQL, plano Free (1 GB) | Vercel Hobby | **R$ 0** | lançar o MVP sem cartão e mostrar no portfólio | a API **dorme** após 15 min sem uso e leva ~1 min para acordar; o Render avisa que o Free não é para produção |
| **B · Sempre ligado** | Railway, plano Hobby | MySQL do próprio Railway (mesmo projeto, rede interna) | Vercel Hobby | ~US$ 5/mês | demo que responde na hora, API e banco no mesmo painel | cobrança por uso: configure o alerta de gasto |
| **C · AWS** | App Runner ou ECS | RDS MySQL | Amplify ou Vercel | variável | aprender cloud (feature 5 do checklist) | precisa do estudo antes; fica para depois do MVP |

**Recomendação:** começar pelo **A** para o 31/10 (custo zero e ninguém precisa pôr cartão) e ir para o **B**
quando a espera de 1 minuto atrapalhar uma apresentação. A imagem é a mesma nos dois: mudar de casa é trocar as
variáveis. A **C** continua como estudo (feature 5).

Por que cabe no plano grátis: medido em 26/10, a API no perfil `prod` usa **~370 MB** dos 512 MB do container.

> ⚠️ **Vercel Hobby é só para uso pessoal e não comercial.** Um banco de portfólio, com dinheiro de mentira, se
> encaixa. Se o RKF Bank virar produto (com receita), o site vai para o plano pago ou para outro provedor.

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
| `DB_HOST` | ✅ | `mysql-xxx.aivencloud.com` | endereço do MySQL |
| `DB_PORT` | | `3306` (padrão) | porta do MySQL (a do Aiven é outra; veja no painel) |
| `DB_NAME` | ✅ | `rkfbank` | nome do banco |
| `DB_USER` / `DB_PASSWORD` | ✅ | do painel do banco | login do banco |
| `DB_SSL_MODE` | | `PREFERRED` (padrão) · `REQUIRED` | use `REQUIRED` quando o banco fica fora da rede do provedor (Aiven) |
| `JWT_SEGREDO` | ✅ | `openssl rand -base64 48` | assina os tokens; **nunca** o mesmo do `.env` de alguém |
| `CORS_ORIGENS` | ✅ | `https://rkfbank.vercel.app` | domínio do site |
| `ADMIN_CPF` / `ADMIN_SENHA` | | CPF válido e senha forte | cria o primeiro ADMIN na primeira subida |
| `JWT_EXPIRACAO` | | `15m` (padrão) | validade do token de acesso |
| `PORT` | | o provedor define | porta HTTP |

Sem as obrigatórias, a API **não sobe** (de propósito: melhor parar do que rodar com segredo errado). Não
existem `EQUIPE_SENHA` nem dados de teste em produção: o banco nasce vazio, só com o schema do Flyway.

Health check do provedor: `GET /actuator/health` (público, responde só `UP`/`DOWN`).

## 5. O site (Vercel)

| Configuração | Valor |
|---|---|
| Root Directory | `frontend` |
| Framework | Next.js (detectado sozinho) |
| Variável `API_URL` | o endereço público da API (ex.: `https://rkfbank-api.onrender.com`) |
| Variável `NEXT_PUBLIC_LOG_LEVEL` | `warn` (opcional) |

Em produção, o cookie de sessão sai com `secure` (só HTTPS), o que a Vercel já garante.

## 6. Depois de publicar, confira

1. `https://<api>/actuator/health` → `UP`.
2. `https://<api>/swagger-ui.html` → `404` (fechado em produção).
3. No site: cadastro (o CEP vira endereço pelo ViaCEP), login, abrir conta, depósito, saque e extrato.
4. `node scripts/smoke-e2e.mjs https://<api>`: 42 verificações. ⚠️ Ele cria clientes de teste no banco de produção:
   rode antes de abrir para o público, ou não rode lá.
5. Primeiro login do ADMIN funcionando; depois, troque a senha dele (`PATCH /usuarios/me/senha`).

## 7. Antes de abrir para o público

- **LGPD:** os encarregados (Kevin e Renan) já estão na seção 7 da política (termos `2026-10-26`). Mudou o texto?
  Suba `TERMOS_VERSAO` (frontend) **e** `LGPD_TERMOS_VERSAO` (variável da API) juntos.
- Deixe claro na tela que é um **banco de demonstração** (dinheiro fictício), para ninguém digitar dado real além do necessário.
- Alerta de custo ligado no provedor (caminho B) e backup do banco (o Aiven Free não promete backup de produção).
