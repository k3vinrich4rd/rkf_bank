// Copia a lista de codigos de erro do backend (CodigoErroEnum) para o tipo CodigoErro do frontend (lib/api/types.ts).
// Uso: node scripts/sincronizar-codigos-erro.mjs      (rode depois de criar um codigo novo no enum)
// O CodigoErroEnumTest falha se as duas listas divergirem: o front nunca fica decidindo por um codigo que nao existe.

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..");
const ENUM = join(RAIZ, "backend/src/main/java/com/br/rkfbank/compartilhado/erros/CodigoErroEnum.java");
const TIPOS = join(RAIZ, "frontend/lib/api/types.ts");

const codigos = [...readFileSync(ENUM, "utf8").matchAll(/^\s+([A-Z_]+)\("/gm)].map((m) => m[1]);
let tipos = readFileSync(TIPOS, "utf8");
const crlf = tipos.includes("\r\n");
tipos = tipos.replace(/\r\n/g, "\n");
const novo = "export type CodigoErro =\n" + codigos.map((c) => `  | "${c}"`).join("\n") + ";";
const atualizado = tipos.replace(/export type CodigoErro =\n(?: {2}\| "[A-Z_]+"\n)* {2}\| "[A-Z_]+";/, novo);
if (atualizado === tipos && !tipos.includes(novo)) {
  console.error("Nao achei o bloco 'export type CodigoErro =' em frontend/lib/api/types.ts");
  process.exit(1);
}
writeFileSync(TIPOS, crlf ? atualizado.replace(/\n/g, "\r\n") : atualizado);
console.log(`✅ ${codigos.length} codigos em frontend/lib/api/types.ts`);
