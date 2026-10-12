import type { LogLevel } from "@/lib/logger";

const NIVEIS_DE_LOG: readonly LogLevel[] = ["debug", "info", "warn", "error"];

function lerLogLevel(valor: string | undefined): LogLevel {
  if (NIVEIS_DE_LOG.includes(valor as LogLevel)) return valor as LogLevel;
  return process.env.NODE_ENV === "production" ? "warn" : "debug";
}

/**
 * Variáveis de ambiente, lidas em um único lugar.
 *
 * `API_URL` é usada só no servidor (proxy do `next.config.ts` e chamadas
 * de Server Components) — por isso não tem o prefixo `NEXT_PUBLIC_`.
 *
 * `NEXT_PUBLIC_LOG_LEVEL` precisa ser lida com o nome literal: o Next só
 * embute no bundle do navegador referências diretas a `process.env.NEXT_PUBLIC_*`.
 */
export const env = {
  apiUrl: process.env.API_URL ?? "http://localhost:8080",
  logLevel: lerLogLevel(process.env.NEXT_PUBLIC_LOG_LEVEL),
} as const;
