# Débitos técnicos

Débito técnico é algo que funciona hoje, mas que vai dar trabalho ou problema no futuro.
Anotar aqui não é reclamar do código: é não esquecer.

**Prioridade:** 🔴 resolver antes do MVP · 🟡 logo depois do MVP · 🟢 quando der · 🔮 futuro (ainda sem plano)

Resolveu um item? Apague a linha no mesmo PR.

---

## Backend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🔴 | O Spring não lê o `.env`. Se as variáveis não estiverem no terminal, ele usa usuário/senha `rkfbank`, que não batem com o Docker. | Adicionar `spring.config.import=optional:file:.env[.properties]` no `application.properties`. |
| 🔴 | O teste `RkfbankApplicationTests` precisa do MySQL ligado para rodar. | Rodar esse teste com o H2 (perfil de teste). |
| 🔴 | O arquivo `application-test.properties` está em `src/main/resources`. | Mover para `src/test/resources`. |
| 🟡 | Não existe login: qualquer pessoa pode ver ou movimentar qualquer conta. | Spring Security + JWT. |
| 🟡 | `GET /clientes` devolve CPF, e-mail e telefone de todos os clientes. | Proteger com login ou remover do uso público. |
| 🟡 | O banco é criado pelo Hibernate (`ddl-auto=update`). | Usar Flyway (scripts de banco versionados). |
| 🟡 | A configuração de CORS vale para `/api/**`, mas as rotas não têm `/api`. Hoje funciona só por causa do proxy do Next. | Ajustar para `/**` ou remover, se o proxy for mantido. |
| 🟡 | Quem chama a API escolhe a agência e o número da conta. | O backend gerar esses números. |
| 🟡 | A paginação devolve o `Page` do Spring direto, e esse formato pode mudar entre versões. | Criar um DTO de página próprio. |
| 🟡 | O `pom.xml` sobrescreve as versões do Tomcat (11.0.27) e do Jackson (3.1.7) por causa de CVEs. | Remover as duas propriedades quando o Spring Boot trouxer versões iguais ou maiores. |
| 🟢 | O `pom.xml` fixa o H2 em 2.3.232 (o 2.4.240 quebra o `CHECK ... IN` das colunas de enum). | Remover quando o H2 corrigir, ou quando os testes usarem MySQL real (Testcontainers). |
| 🟢 | O `pom.xml` fica na raiz, mas o código fica em `backend/`. | Mover `pom.xml` e `mvnw` para `backend/`. |
| 🟢 | E-mail repetido com letras maiúsculas diferentes passa como novo (`Joao@x.com` × `joao@x.com`). | Salvar o e-mail em minúsculas. |
| 🟢 | O enum de lançamento tem `CREDITO` e `DEBITO`, que não são usados. | Remover ou usar. |
| 🟢 | Mensagens de erro misturam texto com e sem acento ("nao", "não"). | Padronizar. |
| 🟢 | Os testes de controller chamam o método direto, sem testar o JSON de verdade. | Usar `MockMvc`. |

## Frontend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🔴 | A abertura de conta sorteia agência (`0001`) e número no navegador e tenta de novo se vier `409` (`components/abrir-conta/use-abrir-conta.ts`). | Apagar quando o backend gerar esses números (ver item do backend). |
| 🔴 | A tela da conta procura a conta página a página em `GET /contas`, porque `GET /contas/{contaId}` ainda não existe (`lib/api/contas.ts`). | Trocar por uma chamada só quando o backend criar o endpoint. |
| 🟡 | Dependências com vulnerabilidades conhecidas (`node scripts/varrer-cves.js frontend`): `next` 16.3.6 (SSRF na otimização de imagem, cache poisoning em SSG/ISR, entre outras) e as transitivas `sharp` 0.35.4, `source-map-js` 1.2.1 e `@modelcontextprotocol/sdk` 1.30.1. O `braces` 3.0.3 ainda não tem correção. | Atualizar `next` e `eslint-config-next` para 16.3.8+, rodar `pnpm update sharp source-map-js @modelcontextprotocol/sdk --depth Infinity` e validar com `scripts/validar.sh frontend`. |
| 🟡 | Não há testes automáticos no frontend. | Vitest + Testing Library. |

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
