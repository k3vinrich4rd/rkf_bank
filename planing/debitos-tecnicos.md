# Débitos técnicos

Débito técnico é algo que funciona hoje, mas que vai dar trabalho ou problema no futuro.
Anotar aqui não é reclamar do código: é não esquecer.

**Prioridade:** 🔴 resolver antes do MVP · 🟡 logo depois do MVP · 🟢 quando der · 🔮 futuro (ainda sem plano)

Resolveu um item? Apague a linha no mesmo PR.

---

## Backend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🔴 | **LGPD:** o cadastro diz "você concorda com os Termos de Uso e a Política de Privacidade", mas os documentos não existem e o aceite não é registrado. | Antes de ir ao ar: escrever a política (dados, finalidade, compartilhamento com o ViaCEP, retenção, contato), `V7__` com `termos_versao` e `termos_aceitos_em`, campo obrigatório no cadastro e encarregado definido. |
| 🟢 | **LGPD:** a anonimização (`TitularService.anonimizarVencidos`) não toca a `descricao` dos lançamentos, que é texto livre e pode ter nome de alguém ("Aluguel do João"). | Limpar a descrição dos lançamentos do titular junto com o resto, ou limitar o texto livre no front. |
| 🟢 | A rotina de anonimização roda em **toda** instância às 03:00. Hoje há uma só; com várias, duas podem processar o mesmo cliente (o resultado é igual, mas é trabalho repetido). | Trava distribuída (ShedLock) quando houver mais de uma instância. |
| 🟢 | **LGPD:** o cadastro devolve o CPF completo e o handler de JSON inválido loga a mensagem do Jackson (pode trazer trecho do valor enviado). | Devolver o CPF mascarado e logar só o tipo da exceção. |
| 🟢 | O limite de tentativas de login fica em memória: reiniciar zera, e com mais de uma instância cada uma conta a sua parte. | Redis ou tabela quando houver deploy com várias instâncias. |
| 🟢 | A senha redefinida pelo ADMIN não obriga a pessoa a trocá-la no próximo login. | Flag `deve_trocar_senha` e bloqueio das outras rotas até a troca. |
| 🟢 | O `ConversorTokenUsuario` faz 1 SELECT por requisição. | Cache de poucos segundos se o volume crescer. |
| 🟢 | `tb_idempotencia` cresce para sempre. | Rotina que apaga chaves com mais de 30 dias (repetições reais acontecem em segundos). |
| 🟡 | Não há *refresh token* nem revogação: o token vale 15 min e "sair" só apaga o token no front; um token vazado vale até expirar. | `POST /auth/refresh` com token de renovação guardado no banco e revogável. |
| 🟡 | A configuração de CORS vale para `/api/**`, mas as rotas não têm `/api`. Hoje funciona só por causa do proxy do Next. | Ajustar para `/**` ou remover, se o proxy for mantido. |
| 🟢 | A conciliação saldo × extrato só roda nos testes (`DadosEquipeIntegracaoTest`). | Rotina agendada ou rota de ADMIN que aponte contas divergentes em produção. |
| 🟡 | A paginação devolve o `Page` do Spring direto, e esse formato pode mudar entre versões. | Criar um DTO de página próprio. |
| 🟡 | O `pom.xml` sobrescreve as versões do Tomcat (11.0.27) e do Jackson (3.1.7) por causa de CVEs. | Remover as duas propriedades quando o Spring Boot trouxer versões iguais ou maiores. |
| 🟢 | O `pom.xml` fixa o H2 em 2.3.232 (o 2.4.240 quebra o `CHECK ... IN` das colunas de enum). | Remover quando o H2 corrigir, ou quando os testes usarem MySQL real (Testcontainers). |
| 🟢 | O `pom.xml` fica na raiz, mas o código fica em `backend/`. | Mover `pom.xml` e `mvnw` para `backend/`. |
| 🟢 | O enum de lançamento tem `CREDITO` e `DEBITO`, que não são usados. | Remover ou usar. |
| 🟢 | Mensagens de erro misturam texto com e sem acento ("nao", "não"). | Padronizar. |
| 🟢 | Os testes de controller chamam o método direto, sem testar o JSON de verdade. | Usar `MockMvc`. |

## Frontend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟡 | Sem *refresh token*: a sessão cai a cada 15 minutos e o usuário precisa entrar de novo (o `401` leva ao login e volta para a tela). | Quando o backend tiver `POST /auth/refresh`, renovar o token no Route Handler do Next antes de vencer. |
| 🟡 | Não há testes automáticos no frontend. | Vitest + Testing Library. |
| 🟢 | O cliente pode ter mais de uma conta, mas as telas usam só a primeira que `GET /contas` devolve (`components/conta/use-conta.ts`). | Seletor de conta quando o produto pedir várias contas. |
| 🟢 | `braces` 3.0.3 ([alerta #7 do Dependabot](https://github.com/k3vinrich4rd/rkf_bank/security/dependabot/7), CVE-2026-93687, **alta**: DoS por estouro de pilha com padrões muito aninhados) **ainda sem correção publicada**. Chega por `micromatch` ← `fast-glob`, usados pelo `eslint-config-next` e pelo CLI `shadcn` (`ts-morph`). Fica em 🟢 apesar da gravidade porque só roda no lint e nas ferramentas da máquina de quem desenvolve: não vai para o build do site e nunca recebe entrada de usuário. Revisado em 21/10/2026. | Rodar `node scripts/varrer-cves.js frontend` de vez em quando e atualizar quando sair a correção (ou quando `eslint-config-next`/`shadcn` trocarem de dependência). Área do Renan. |
| 🟡 | `hono` 4.13.9 ([alerta #17 do Dependabot](https://github.com/k3vinrich4rd/rkf_bank/security/dependabot/17), CVE-2026-111757, moderada: `serveStatic` decodifica o caminho duas vezes e pula middleware) chega pelo CLI `shadcn` → `@modelcontextprotocol/sdk` → `@hono/node-server`. O site não usa o Hono, mas **a correção já existe** (4.13.11) e custa uma linha. Revisado em 21/10/2026. | `pnpm.overrides` com `"hono": "^4.13.11"` no `frontend/package.json` (ou subir o `shadcn` quando ele trouxer a versão nova), `pnpm install`, `scripts/validar.sh frontend` e conferir que o alerta fecha. Área do Renan. |

## Projeto

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟡 | O frontend não tem CI (o backend já tem: build, testes, CodeQL e revisão de dependências). | Workflow rodando `lint`, `typecheck` e `build` do frontend nos PRs. |
| 🟡 | O Dependabot só olha o backend (Maven) e as GitHub Actions. | Adicionar o ecossistema `npm` apontando para `frontend/`. |
| 🟢 | A pasta `planing` está escrita errado (o certo é `planning`). | Renomear quando ninguém estiver mexendo nela. |

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
