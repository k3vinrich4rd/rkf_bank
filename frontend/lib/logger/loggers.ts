import { createLogger } from "./logger";

/**
 * Loggers prontos de cada camada da API. Importe o da camada em que você está:
 *
 *   import { logContas } from "@/lib/logger";
 *   logContas.info("Conta aberta", { contaId });
 *
 * Novo recurso da API (ex.: cartões)? Adicione uma linha aqui.
 * Para telas, use `useLogDeTela` / `<LogDeTela />` (em `components/logs`).
 */

/** Transporte HTTP: método, rota, status e tempo de cada chamada. */
export const logHttp = createLogger("api:http");

/** Operações de cliente (`lib/api/clientes.ts`). */
export const logClientes = createLogger("api:clientes");

/** Operações de conta, movimentação e extrato (`lib/api/contas.ts`). */
export const logContas = createLogger("api:contas");

/** Login, logout e sessão (`lib/api/auth.ts`). */
export const logAuth = createLogger("api:auth");
