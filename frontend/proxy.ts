import { NextResponse, type NextRequest } from "next/server";
import type { LoginResponse } from "@/lib/api";
import {
  COOKIE_REFRESH,
  COOKIE_SESSAO,
  COOKIE_TOKEN,
  apagarSessao,
  gravarSessao,
  lerSessao,
  sessaoDoLogin,
} from "@/lib/auth/cookies";
import { precisaRenovar, renovarSessao } from "@/lib/auth/renovar";
import { ROUTES } from "@/lib/routes";

/**
 * Roda no servidor do Next antes de cada requisição do `matcher` (no Next 16, o antigo "middleware").
 *
 * 0. **Renovação (contrato 3.1):** se o token de acesso venceu ou está para vencer e existe o de renovação, troca os
 *    dois na API (`POST /auth/refresh`) antes de seguir. A pessoa não percebe: nada de cair no login a cada 15 min.
 * 1. `/api/*` → lê o token do cookie `httpOnly` e envia para a API como `Authorization: Bearer`.
 *    O rewrite do `next.config.ts` repassa esse header ao backend; o navegador nunca vê o token.
 * 2. `/conta/*` sem sessão → manda para o login, voltando para a mesma tela depois.
 *    É uma checagem otimista: quem valida o token de verdade é a API (token vencido → 401 → login).
 */
export async function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;

  // Login, logout e troca de senha são rotas do próprio Next e cuidam dos cookies sozinhas. A de sessão passa pela
  // renovação abaixo: é ela que a tela consulta para saber se ainda está logada.
  if (pathname.startsWith("/api/auth/") && pathname !== "/api/auth/sessao") return NextResponse.next();

  let token = request.cookies.get(COOKIE_TOKEN)?.value;
  const refresh = request.cookies.get(COOKIE_REFRESH)?.value;
  let renovada: LoginResponse | null = null;
  let renovacaoRecusada = false;

  if (refresh && precisaRenovar(token, lerSessao(request.cookies.get(COOKIE_SESSAO)?.value)?.expiraEm)) {
    renovada = await renovarSessao(refresh);
    if (renovada) {
      token = renovada.token;
      // A requisição que segue (rota do Next ou API) já enxerga a sessão nova.
      request.cookies.set(COOKIE_TOKEN, renovada.token);
      request.cookies.set(COOKIE_SESSAO, JSON.stringify(sessaoDoLogin(renovada)));
    } else if (!token) {
      renovacaoRecusada = true;
    }
  }

  let resposta: NextResponse;
  if (renovada && pathname.startsWith("/api/") && pathname !== "/api/auth/sessao") {
    // O rewrite externo do next.config.ts descarta o Set-Cookie do proxy: sem isto o navegador ficaria com o token
    // de renovação antigo, já trocado, e a próxima renovação contaria como reuso (a API derruba a sessão inteira).
    // O 307 repete a mesma chamada (mesmo método e corpo) já com os cookies novos.
    resposta = NextResponse.redirect(request.nextUrl, 307);
  } else if (pathname === "/api/auth/sessao") {
    resposta = NextResponse.next({ request: { headers: request.headers } });
  } else if (pathname.startsWith("/api/")) {
    const headers = new Headers(request.headers);
    // O backend não precisa dos cookies do site; e só o servidor decide o header de autenticação.
    headers.delete("cookie");
    headers.delete("authorization");
    if (token) headers.set("authorization", `Bearer ${token}`);
    resposta = NextResponse.next({ request: { headers } });
  } else if (!token) {
    const login = new URL(ROUTES.login, request.url);
    login.searchParams.set("voltar", `${pathname}${search}`);
    resposta = NextResponse.redirect(login);
  } else {
    resposta = NextResponse.next({ request: { headers: request.headers } });
  }

  // O navegador guarda o par novo; renovação recusada (vencida, revogada) apaga a sessão que sobrou.
  if (renovada) gravarSessao(resposta.cookies, renovada);
  else if (renovacaoRecusada) apagarSessao(resposta.cookies);
  return resposta;
}

export const config = {
  matcher: ["/api/:path*", "/conta", "/conta/:path*"],
};
