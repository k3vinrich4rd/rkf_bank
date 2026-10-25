import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * Testes do frontend (Vitest + Testing Library). `pnpm test` roda uma vez (CI); `pnpm test:watch` fica olhando.
 * Os testes moram ao lado do código, em `*.test.ts(x)`. O ambiente padrão é Node (lógica de sessão e o `proxy.ts`);
 * teste de componente pede o navegador simulado com o comentário `// @vitest-environment jsdom` no topo do arquivo.
 */
export default defineConfig({
  resolve: {
    alias: { "@": fileURLToPath(new URL("./", import.meta.url)) },
  },
  test: {
    environment: "node",
    include: ["**/*.test.{ts,tsx}"],
    exclude: ["node_modules/**", ".next/**"],
    restoreMocks: true,
    unstubGlobals: true,
  },
});
