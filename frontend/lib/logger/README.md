# Logs do frontend

> Voltar para o [README do frontend](../../README.md).

**Por que ter um logger e não usar `console.log`?** Para que todo log tenha o mesmo formato (fácil de filtrar),
respeite um nível mínimo por ambiente (menos ruído em produção) e **nunca vaze dado pessoal** (LGPD) —
sem que cada dev precise lembrar disso.

Todo log sai no console no formato `[rkf][escopo] mensagem { contexto }`.
No navegador aparece no DevTools; em Server Components, no terminal do `pnpm dev`.

Exemplo real de um cadastro com CPF repetido:

```text
[rkf][tela:cadastro] Formulário enviado
[rkf][api:clientes] Cadastrando cliente { cpf: "***.***.247-25", email: "j***@rkfbank.com" }
[rkf][api:http] POST /clientes → 409: CPF ja cadastrado { requestId: "…", duracaoMs: 212 }
[rkf][tela:cadastro] Cadastro não concluído { status: 409, erro: "CPF ja cadastrado", mensagemExibida: "CPF ja cadastrado" }
```

**Dica:** no DevTools, filtre por `[rkf]` para ver só os logs do app, ou por `requestId` para seguir uma chamada.

## Qual usar?

| Estou em… | Use | Import |
|---|---|---|
| Página que só precisa registrar que foi aberta | `<LogDeTela tela="…" />` | `@/components/logs` |
| Tela com ações do usuário (form, botões) | `useLogDeTela("…")` | `@/components/logs` |
| Função de API em `lib/api/` | `logHttp`, `logClientes`, `logContas` | `@/lib/logger` |
| Algo que não se encaixa acima | `createLogger("escopo")` | `@/lib/logger` |

### Página simples (pode ser Server Component)

```tsx
import { LogDeTela } from "@/components/logs";

export default function ExtratoPage() {
  return (
    <>
      <LogDeTela tela="extrato" />
      <ExtratoScreen />
    </>
  );
}
```

### Tela com ações

```tsx
const logTela = useLogDeTela("transferencia"); // já loga "Tela aberta"

logTela.acao("Transferência enviada", { contaDestinoId });
logTela.sucesso("Transferência concluída");
logTela.falha("Transferência não concluída", error); // error vira { status, erro }
```

Exemplo real: `components/cadastro/use-cadastro-form.ts`.

### Camada de API

```ts
import { logContas } from "@/lib/logger";

logContas.info("Conta aberta", { contaId });
```

O `apiRequest` já loga toda chamada HTTP (método, rota, status, tempo e `requestId`).
**Não logue de novo o erro HTTP** na tela; registre só o que o usuário viu.

## Níveis

| Nível | Quando |
|---|---|
| `debug` | detalhe técnico (corpo da request, tela fechada) |
| `info` | algo aconteceu como esperado (tela aberta, conta criada) |
| `warn` | falha esperada: validação, regra de negócio, erro 4xx |
| `error` | falha do sistema: 5xx, backend fora do ar, rede |

Nível mínimo: `NEXT_PUBLIC_LOG_LEVEL` no `.env` (padrão: `debug` em dev, `warn` em produção).

## Dados pessoais (LGPD)

`cpf`, `email`, `telefone` e `nomeCompleto` são mascarados automaticamente em qualquer
nível do contexto. Pode passar o objeto inteiro sem medo. Para proteger um campo novo,
adicione-o em `lib/logger/mascara.ts`.

## Organização

```
lib/logger/           ← sem React: funciona no servidor e no navegador
  logger.ts           núcleo: níveis, formato, saída
  loggers.ts          loggers prontos das camadas da API
  mascara.ts          mascaramento LGPD
  erro.ts             resume um erro em { status, erro }
  index.ts            ponto único de import
components/logs/      ← React: logs de tela
  use-log-de-tela.ts  hook
  log-de-tela.tsx     componente
```

Nova camada da API (ex.: cartões) → adicione `logCartoes` em `loggers.ts`.
Nova tela → só use o hook ou o componente; não precisa criar nada.
