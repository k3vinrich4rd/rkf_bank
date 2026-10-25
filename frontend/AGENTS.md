<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Contexto do projeto RKF Bank (frontend)

Leia antes de alterar código: [README.md](README.md) e o contrato em
[../backend/README_CONTRATO_API.md](../backend/README_CONTRATO_API.md).

## Regras

- **Nada de `fetch` em componentes.** Chamadas à API passam por `clientesApi` / `contasApi` (`lib/api`),
  que usam `apiRequest` (`lib/api/client.ts`).
- **Tipos da API** vivem em `lib/api/types.ts` e espelham o contrato. Mudou o contrato → mude os tipos.
- **Uma tela = página + screen + hook.** `app/<rota>/page.tsx` (Server Component) renderiza
  `components/<tela>/<tela>-screen.tsx` (Client Component), e a lógica fica em `components/<tela>/use-*.ts`.
- **Erros da API:** decida pelo `ApiError.status`; use `error.fieldErrors` para marcar campos do formulário.
- **Dinheiro:** a API trafega reais em decimal; converta com `decimalToCents` e exiba com `formatCurrency`.
- **Logs:** `useLogDeTela` / `<LogDeTela />` nas telas e os loggers de `lib/logger` na camada de API.
  Nunca use `console.log` direto. Dados pessoais são mascarados automaticamente.
- **Rotas** em `lib/routes.ts`; **variáveis de ambiente** só via `lib/env.ts`.
- **Estilo:** Tailwind 4 com os tokens da marca de `app/globals.css` (`brand-blue`, `brand-cyan`, `surface`).
- **Idioma:** domínio em português (`Conta`, `depositar`, `useCadastroForm`).
- **Antes de entregar:** `pnpm lint`, `pnpm typecheck` e `pnpm build` sem erros.
