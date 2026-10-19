import { NextResponse, type NextRequest } from "next/server";
import { COOKIE_TOKEN } from "@/lib/auth/cookies";
import { ROUTES } from "@/lib/routes";

/**
 * Roda no servidor do Next antes de cada requisição do `matcher` (no Next 16, o antigo "middleware").
 *
 * 1. `/api/*` → lê o token do cookie `httpOnly` e envia para a API como `Authorization: Bearer`.
 *    O rewrite do `next.config.ts` repassa esse header ao backend; o navegador nunca vê o token.
 * 2. `/conta/*` sem cookie → manda para o login, voltando para a mesma tela depois.
 *    É uma checagem otimista: quem valida o token de verdade é a API (token vencido → 401 → login).
 */
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const token = request.cookies.get(COOKIE_TOKEN)?.value;

  if (pathname.startsWith("/api/")) {
    // As rotas de login, logout e sessão são do próprio Next e cuidam dos cookies sozinhas.
    if (pathname.startsWith("/api/auth/")) return NextResponse.next();

    const headers = new Headers(request.headers);
    // O backend não precisa dos cookies do site; e só o servidor decide o header de autenticação.
    headers.delete("cookie");
    headers.delete("authorization");
    if (token) headers.set("authorization", `Bearer ${token}`);
    return NextResponse.next({ request: { headers } });
  }

  if (!token) {
    const login = new URL(ROUTES.login, request.url);
    login.searchParams.set("voltar", `${pathname}${search}`);
    return NextResponse.redirect(login);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/api/:path*", "/conta", "/conta/:path*"],
};
