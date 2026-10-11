/**
 * Variáveis de ambiente, lidas em um único lugar.
 *
 * `API_URL` é usada só no servidor (proxy do `next.config.ts` e chamadas
 * de Server Components) — por isso não tem o prefixo `NEXT_PUBLIC_`.
 */
export const env = {
  apiUrl: process.env.API_URL ?? "http://localhost:8080",
} as const;
