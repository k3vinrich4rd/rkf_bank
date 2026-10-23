import { cookies } from "next/headers";
import type { ErroResponse } from "@/lib/api";
import { COOKIE_SESSAO, COOKIE_TOKEN, lerSessao } from "@/lib/auth/cookies";

/** Quem está logado (cliente, perfil e validade), para o navegador que não consegue ler os cookies `httpOnly`. */
export async function GET() {
  const cookieStore = await cookies();
  const sessao = cookieStore.has(COOKIE_TOKEN) ? lerSessao(cookieStore.get(COOKIE_SESSAO)?.value) : null;

  if (!sessao) {
    const corpo: ErroResponse = {
      timestamp: new Date().toISOString(),
      status: 401,
      error: "Unauthorized",
      codigo: "TOKEN_INVALIDO",
      message: "Sessão expirada; faça login novamente",
      path: "/auth/sessao",
      campos: [],
    };
    return Response.json(corpo, { status: 401 });
  }

  return Response.json(sessao);
}
