---
name: nova-tela
description: Cria uma tela nova no frontend do RKF Bank seguindo o padrão do time (página Server Component + screen Client Component + hook), com rota em lib/routes.ts, chamadas pela camada lib/api, logs de tela e validação com lint, typecheck e build. Use quando pedirem "/nova-tela <nome>", "cria a tela de extrato", "nova página de depósito" ou similar.
---

# /nova-tela <nome>

> 🧑‍🎨 O frontend é responsabilidade do **Renan**. Se quem pediu não for ele, confirme antes de criar e avise que a tela vai precisar da revisão dele.

Antes de tudo, leia `frontend/CLAUDE.md` e `frontend/AGENTS.md`: o Next.js deste projeto (16) tem APIs diferentes das versões antigas. Na dúvida, consulte `frontend/node_modules/next/dist/docs/`, não a memória.

## 1. Entender a tela
- Qual rota (ex.: `/conta/extrato`), quais dados mostra e quais ações tem.
- Quais endpoints usa: confira em `backend/README_CONTRATO_API.md`. Se o endpoint não existir, pare e sugira um `TODO(backend)` + `/recado` para o Kevin.

## 2. Arquivos (padrão "uma tela = página + screen + hook")

| Arquivo | Papel | Modelo a copiar |
|---|---|---|
| `frontend/app/<rota>/page.tsx` | Server Component, só `metadata` e `<XScreen />` | `app/conta/page.tsx` |
| `frontend/components/<tela>/<tela>-screen.tsx` | Client Component (`"use client"`), renderiza por **etapa** | `components/conta/conta-screen.tsx` |
| `frontend/components/<tela>/use-<tela>.ts` | estado, efeitos e chamadas à API; exporta o tipo das etapas | `components/conta/use-conta.ts` |
| `frontend/lib/routes.ts` | nova entrada em `ROUTES` | — |
| `frontend/lib/api/*.ts` e `types.ts` | só se faltar a chamada ou o tipo; tipos espelham o contrato | `lib/api/contas.ts` |

## 3. Regras obrigatórias
- **Nada de `fetch` em componente:** use `clientesApi` / `contasApi` (`lib/api`).
- Erros pela `ApiError.status`; `error.fieldErrors` para marcar campos.
- Dinheiro: `decimalToCents` para conta, `formatCurrency` para exibir.
- Logs: `useLogDeTela` / `<LogDeTela />`; nunca `console.log`.
- Rotas só via `ROUTES`; variáveis de ambiente só via `lib/env.ts`.
- Tailwind 4 com os tokens da marca (`brand-blue`, `brand-cyan`, `surface`); componentes de `components/ui`.
- Nomes de domínio em português (`ExtratoScreen`, `useExtrato`).
- Etapas explícitas no hook (ex.: `carregando | erro | vazio | pronta`), como em `use-conta.ts`.

## 4. Validar e documentar
- `scripts/validar.sh frontend` (lint, typecheck e build) até ficar verde.
- Atualizar a tabela "🗺️ Telas" do `frontend/README.md`.
- Commit `feat: adiciona tela de <nome>`; se houver `TODO(backend)`, registrar em `planing/debitos-tecnicos.md` (seção Frontend).
