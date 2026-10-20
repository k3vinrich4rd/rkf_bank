# Decisões

Quando o time decidir algo importante, anote aqui. Assim ninguém precisa lembrar "por que fizemos assim".

**Modelo:**

```
## Título
Data:
Decisão:
Por quê:
```

---

## Stack do projeto
Data: 25/09
Decisão: backend em Java + Spring Boot; frontend em Next.js + TypeScript.
Por quê: é a stack que o time quer aprender e usar no mercado.

## Banco de dados: MySQL
Data: 02/10
Decisão: usar MySQL 8.4 rodando no Docker (e não PostgreSQL, como pensado no começo).
Por quê: foi o banco usado na implementação do backend. Trocar agora atrasaria o MVP.

## Banco versionado com Flyway
Data: 15/10
Decisão: o schema passa a ser criado e alterado só por migrações do Flyway (`backend/src/main/resources/db/migration`); o Hibernate fica em `ddl-auto=validate`. Bancos locais antigos são recriados uma vez (`docker compose down -v`), sem *baseline*. UUID fixado como `BINARY(16)` no Hibernate e e-mail salvo em minúsculas (`V2`).
Por quê: histórico do banco no Git, revisado em PR e igual em todo ambiente. Sem produção ainda, recriar é mais seguro que o *baseline*, que esconderia as diferenças do schema antigo (FKs com nome aleatório, sem índices). O `BINARY` e o SQL portável fazem o H2 dos testes validar o mesmo schema do MySQL.

## Contrato da API
Data: 02/10
Decisão: o arquivo `backend/README_CONTRATO_API.md` é a regra entre o backend e o frontend.
Por quê: assim um lado não fica adivinhando o que o outro faz. Se mudar um endpoint, atualize o arquivo no mesmo PR.

## Rotas da API seguem o backend atual
Data: 11/10
Decisão: as rotas não têm prefixo `/api`. Abrir conta é `POST /contas/clientes/{clienteId}` e transferir é `POST /contas/transferencia`.
Por quê: o backend já estava pronto e testado assim; ajustar o frontend e o contrato era mais barato perto do prazo.

## Frontend chama o backend por um proxy
Data: 11/10
Decisão: o navegador chama `/api/...` no próprio Next, que repassa para o backend.
Por quê: evita problemas de CORS e não expõe o endereço do backend.

## Logs do frontend com dados mascarados
Data: 12/10
Decisão: todo log do frontend passa por `lib/logger`, que esconde CPF, e-mail, telefone e nome.
Por quê: ajuda a investigar erros sem expor dados pessoais (LGPD).

## Login com CPF + senha e JWT
Data: 16/10
Decisão: o cliente entra com **CPF + senha** e recebe um **JWT** (HS256, expira em 15 min por padrão, sem refresh token por enquanto). A API valida o token com o **OAuth2 Resource Server** do Spring (não com filtro próprio). Senha e perfil (`CLIENTE`/`ADMIN`) ficam em `tb_usuario` (V3), separada de `tb_cliente`, com a senha em **BCrypt**. O primeiro `ADMIN` é criado na subida a partir de `ADMIN_CPF`/`ADMIN_SENHA` do `.env`. Públicas só `POST /clientes` e `POST /auth/login`; `GET /clientes` é só do ADMIN; em `GET /contas` o ADMIN vê todas e o CLIENTE só as dele; movimentar, ver extrato e abrir conta só nas contas do próprio cliente (`403`).
Por quê: CPF já é único e validado no cadastro. O Resource Server é a forma oficial e testada de validar JWT (menos código nosso para errar). Tabela separada deixa credencial longe do dado cadastral e permite admin que não é cliente. Admin pelo `.env` evita senha em migração ou no Git. Também responde ao "acesso à conta sem login": agora o usuário volta pelo login.

---

## Onde o frontend guarda o token do login
Data: 17/10
Decisão: o token fica num **cookie `httpOnly`** gravado pelo servidor do Next (padrão BFF, *Backend for Frontend*). O login passa por `POST /api/auth/login` (Route Handler do Next), que chama a API, guarda o token no cookie `rkf_token` e devolve ao navegador só `{clienteId, perfil, expiraEm}`. Nas outras chamadas, o `proxy.ts` lê o cookie e envia `Authorization: Bearer` para a API. Cookies com `SameSite=Lax`, `Secure` em produção e validade igual à do token. `401` no meio do uso leva ao login (`?expirou=1`) e depois de volta para a tela.
Por quê: com `localStorage`/`sessionStorage`, qualquer script que rode na página (um XSS, uma dependência comprometida) lê e rouba o token; com `httpOnly`, o JavaScript não alcança o cookie. O proxy `/api` já existia, então o custo foi pequeno. `SameSite=Lax` evita que outro site faça POST com o cookie (CSRF). Alternativa descartada: `sessionStorage` (mais simples, mas exposta a XSS).

## Papéis do time
Data: 19/10
Decisão: o RKF Bank foi **idealizado pelos três**: Kevin Richard, Renan Barros e Felipe Santana. Kevin cuida do backend, Renan do frontend e **Felipe** é o PO & QA e o **primeiro cliente** (tem acesso antes de todo mundo para os primeiros testes). O time inteiro é de Osasco, SP. (Atualizado em 19/10: antes só o Felipe constava como idealizador.)
Por quê: deixa claro quem decide prioridade de produto e quem dá o aceite do MVP.

## LGPD: mínimo antes do deploy, resto depois do MVP
Data: 19/10
Decisão: o roadmap técnico segue (Testcontainers → Swagger). O "LGPD mínimo" (política de privacidade, registro do aceite dos termos e encarregado) **bloqueia o "Colocar no ar"**, como o login bloqueou. Direitos do titular (ver, corrigir, encerrar) entram depois do MVP, com contrato combinado com o frontend.
Por quê: hoje só há dados de teste no ambiente local, então o risco real começa no deploy. Os itens mínimos são pequenos e não competem com as features; os direitos do titular são features de produto que precisam de tela.

## Flyway reestruturado e contas de demonstração
Data: 20/10
Decisão: as migrações viram uma tabela por versão (`V1__criar_tabela_endereco` a `V5__criar_tabela_usuario`), substituindo `V1__schema_inicial`, `V2__email_minusculo` e `V3__usuarios`. Dados prontos ficam em callbacks `afterMigrate` do Flyway, que rodam a cada subida e só inserem o que falta: **Felipe Santana** (conta de origem, R$ 1.000,00) e **Renan Barros** (conta de destino, R$ 500,00), em `db/seed/equipe`. Ligados no perfil padrão; desligados no `test` e em produção (`FLYWAY_LOCATIONS=classpath:db/migration`). (Atualizado no mesmo dia: o login dessas contas saiu do Git, ver "Acesso da equipe × visitantes".) Cada máquina recria o MySQL local uma vez (`docker compose down -v`).
Por quê: sem produção, reorganizar agora é barato e deixa o histórico legível (a `V2` só corrigia dados de um banco que ninguém mais tem). Contas prontas tiram o ViaCEP e o cadastro do caminho de quem só quer testar, e ids fixos deixam o guia de testes igual em toda máquina. Callback em vez de `V6__`: roda sempre (recria o que foi apagado), não entra no histórico e pode ser ajustado sem quebrar checksum. Seed fora do `test` para os testes continuarem partindo de banco vazio. Daqui para frente, migração aplicada volta a ser intocável.

## Movimentações idempotentes (Idempotency-Key)
Data: 20/10
Decisão: depósito, saque e transferência exigem o header `Idempotency-Key` (contrato 2.1). O `IdempotenciaService` grava a chave em `tb_idempotencia` (V6) **na mesma transação** da movimentação, com o SHA-256 da operação; repetir a chave com o mesmo corpo devolve `204` + `Idempotency-Replayed: true` sem movimentar; com outro corpo, `422`; sem chave, `400`. O frontend gera a chave ao enviar e a reusa nas novas tentativas do mesmo pedido.
Por quê: sem isso, um retry depois de a rede cair debitava duas vezes. Obrigatório (e não opcional) porque proteção opcional não protege quem esquece de mandar. Padrão de mercado (Stripe, Open Finance). A trava final é o `UNIQUE (usuario_id, chave)`: duas chamadas simultâneas não gravam as duas.

## Conta não se apaga
Data: 20/10
Decisão: `Cliente.contas` passa de `cascade = ALL, orphanRemoval = true` para `cascade = PERSIST`. Cliente com conta não pode ser excluído (FK); conta se encerra com `ativa = false`.
Por quê: tirar a conta da lista do cliente **apagava** a conta, e o extrato (sem FK) ficava apontando para o nada. Em banco, histórico financeiro não some.

## Número de conta gerado pelo banco
Data: 20/10
Decisão: `agencia` e `numeroConta` viram opcionais na abertura; sem eles, a API usa a agência `0001` e gera 8 dígitos (7 aleatórios com `SecureRandom` + DV módulo 11), tentando de novo em caso de colisão. O frontend parou de sortear.
Por quê: número de conta é identidade do banco. Aleatório (e não sequencial) para não dar para adivinhar contas vizinhas.

## Login com limite de tentativas e health check
Data: 20/10
Decisão: 5 logins errados para o mesmo CPF em 15 min bloqueiam aquele CPF (`429` + `Retry-After`), em memória (`LimitadorTentativasLogin`). `GET /actuator/health` público, só `UP`/`DOWN`.
Por quê: freia força bruta sem infraestrutura nova; com mais de uma instância, migrar para Redis ou tabela. O health é pré-requisito do deploy.

## Acesso da equipe × visitantes
Data: 20/10
Decisão: o repositório é público, então **nenhuma senha real fica no Git**. As contas do Felipe e do Renan (seed `db/seed/equipe`) não têm usuário no SQL: o `CriadorAcessoEquipe` cria o login na subida com a `EQUIPE_SENHA` do `.env`, como o admin. Os acessos e o roteiro ponta a ponta da equipe ficam em `.equipe/GUIA_TESTES_EQUIPE.md`, no `.gitignore`, compartilhado só por canal privado entre os três. Para quem chega pelo GitHub há o perfil `visitante`: H2, contas fictícias (Ana e Bruno) com senha pública, **sem** admin e sem contas da equipe, segredo do JWT sorteado a cada subida e `.env` ignorado para segredos. O perfil `demo` (que tinha segredo e senhas fixos) foi removido antes de ir para o Git, e o `application-test.properties` saiu do jar (`src/test/resources`). O Spring passa a ler o `.env` sozinho.
Por quê: documentação pública com senha e ids das contas da equipe expunha o ambiente de teste do time; um segredo de JWT escrito no Git deixa qualquer leitor emitir token de admin. O visitante continua conseguindo testar tudo, sem nada que dê acesso ao que é do time.

## Gestão de usuários e revogação imediata
Data: 20/10
Decisão: `tb_usuario.ativo` (V7) e rotas só de ADMIN em `/admin/usuarios` para listar, promover/rebaixar e desativar/reativar. A cada requisição, o `ConversorTokenUsuario` (último passo do Resource Server) confere se o usuário do token existe, está ativo e tem o mesmo perfil do token; se não, `401`. Ninguém altera o próprio perfil ou acesso. Usuário não é apagado: "remover" é desativar. Admin "puro" continua nascendo só do `.env`.
Por quê: com JWT puramente *stateless*, rebaixar ou desativar só valeria quando o token vencesse (até 60 min no teste). O custo da conferência é 1 SELECT pela chave primária por requisição, aceitável para o tamanho do banco; com escala, cachear por alguns segundos. A trava de "não mexer em si mesmo" impede o admin de se trancar para fora e garante que sobra sempre um ADMIN ativo.

## Troca e redefinição de senha
Data: 20/10
Decisão: `PATCH /auth/senha` troca a própria senha exigindo a atual (erro conta no limite de tentativas do login); `PATCH /admin/usuarios/{id}/senha` deixa o ADMIN redefinir a senha de quem esqueceu (nunca a própria). Toda troca sobe `tb_usuario.versao_credencial` (V8); o token leva a claim `versaoCredencial` e o `ConversorTokenUsuario` recusa versão diferente, então os tokens anteriores caem na hora. Nas contas da equipe, a `EQUIPE_SENHA` do `.env` volta a valer na próxima subida.
Por quê: senha que não se troca é débito de segurança (vazou, não há o que fazer). Contador e não data de troca: o `iat` do JWT só tem segundos, e uma comparação por horário deixaria passar o token emitido no mesmo segundo da troca.

---

## Ainda falta decidir

- [x] **Acesso à conta sem login:** resolvido com o login (ver "Login com CPF + senha e JWT").
- [ ] **Dinheiro:** a API manda o valor em reais (`50.00`) ou em centavos (`5000`)? Hoje a API usa reais e o frontend converte para centavos.
- [x] **Login:** feito (feature 2 do checklist).
- [ ] **Onde publicar:** onde o site, a API e o banco vão ficar no ar?
- [ ] **Arquitetura alvo:** monólito modular por funcionalidade (`cliente`, `conta`, `auth`, `pix`) a partir do PR do Pix? Proposta e motivos em [README_IMPLEMENTACAO.md, seção 8](../backend/README_IMPLEMENTACAO.md#8-revisão-de-arquitetura-20102026).
