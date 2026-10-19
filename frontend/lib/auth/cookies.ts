import type { Perfil, Sessao } from "@/lib/api";

/**
 * Cookies da sessão. Só o servidor do Next lê e escreve (Route Handlers em `app/api/auth` e o `proxy.ts`):
 * os dois são `httpOnly`, então nenhum JavaScript da página consegue ler o token, nem um script injetado (XSS).
 * Decisão registrada em planing/decisoes.md ("Onde o frontend guarda o token").
 */
export const COOKIE_TOKEN = "rkf_token";
/** O que o navegador pode saber da sessão (cliente, perfil e validade), sem o token. */
export const COOKIE_SESSAO = "rkf_sessao";

/**
 * - `httpOnly`: fora do alcance do `document.cookie`;
 * - `sameSite: "lax"`: o navegador não envia o cookie em POST vindo de outro site (proteção contra CSRF);
 * - `secure`: só HTTPS em produção (em `localhost` o navegador aceita HTTP);
 * - `maxAge`: o cookie some junto com o token, que vale `expiraEmSegundos`.
 */
export function opcoesDoCookie(maxAgeSegundos: number) {
  return {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: maxAgeSegundos,
  } as const;
}

const PERFIS: readonly Perfil[] = ["CLIENTE", "ADMIN"];

/** Lê o cookie da sessão. Conteúdo ausente ou estranho conta como "sem sessão". */
export function lerSessao(valor: string | undefined): Sessao | null {
  if (!valor) return null;
  try {
    const sessao: unknown = JSON.parse(valor);
    if (
      typeof sessao === "object" &&
      sessao !== null &&
      "perfil" in sessao &&
      PERFIS.includes(sessao.perfil as Perfil) &&
      "expiraEm" in sessao &&
      typeof sessao.expiraEm === "string"
    ) {
      return sessao as Sessao;
    }
  } catch {
    // Cookie corrompido: trata como sem sessão.
  }
  return null;
}
