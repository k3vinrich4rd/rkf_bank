import type { LoginResponse, Perfil, Sessao } from "@/lib/api";

/**
 * Cookies da sessão. Só o servidor do Next lê e escreve (Route Handlers em `app/api/auth` e o `proxy.ts`):
 * os dois são `httpOnly`, então nenhum JavaScript da página consegue ler o token, nem um script injetado (XSS).
 * Decisão registrada em planing/decisoes.md ("Onde o frontend guarda o token").
 */
export const COOKIE_TOKEN = "rkf_token";
/** O que o navegador pode saber da sessão (cliente, perfil e validade), sem o token. */
export const COOKIE_SESSAO = "rkf_sessao";
/** Token de renovação (contrato 3.1): vive 7 dias e troca o de acesso sem pedir a senha. */
export const COOKIE_REFRESH = "rkf_refresh";

/** Onde gravar ou apagar cookies: o `cookies()` de um Route Handler ou o `response.cookies` do `proxy.ts`. */
interface CookiesGravaveis {
  set(nome: string, valor: string, opcoes: ReturnType<typeof opcoesDoCookie>): unknown;
  delete(nome: string): unknown;
}

/** A parte da resposta do login que o navegador pode saber (sem os tokens). */
export function sessaoDoLogin(login: LoginResponse): Sessao {
  return {
    clienteId: login.clienteId,
    perfil: login.perfil,
    expiraEm: new Date(Date.now() + login.expiraEmSegundos * 1000).toISOString(),
    deveTrocarSenha: login.deveTrocarSenha,
  };
}

/**
 * Grava os três cookies depois do login ou de uma renovação. O de acesso some quando o token vence; o da sessão e o
 * de renovação duram o que a renovação dura, para o `proxy.ts` conseguir renovar mesmo com o de acesso já vencido.
 */
export function gravarSessao(cookies: CookiesGravaveis, login: LoginResponse) {
  cookies.set(COOKIE_TOKEN, login.token, opcoesDoCookie(login.expiraEmSegundos));
  cookies.set(COOKIE_REFRESH, login.refreshToken, opcoesDoCookie(login.refreshExpiraEmSegundos));
  cookies.set(COOKIE_SESSAO, JSON.stringify(sessaoDoLogin(login)), opcoesDoCookie(login.refreshExpiraEmSegundos));
}

export function apagarSessao(cookies: CookiesGravaveis) {
  cookies.delete(COOKIE_TOKEN);
  cookies.delete(COOKIE_REFRESH);
  cookies.delete(COOKIE_SESSAO);
}

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
      // Cookie gravado antes do contrato 3.1 não tem `deveTrocarSenha`: conta como `false`.
      return { ...(sessao as Sessao), deveTrocarSenha: "deveTrocarSenha" in sessao && sessao.deveTrocarSenha === true };
    }
  } catch {
    // Cookie corrompido: trata como sem sessão.
  }
  return null;
}
