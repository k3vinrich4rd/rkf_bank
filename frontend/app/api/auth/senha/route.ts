import { cookies } from "next/headers";
import type { ErroResponse, TrocarSenhaRequest } from "@/lib/api";
import { COOKIE_SESSAO, COOKIE_TOKEN } from "@/lib/auth/cookies";
import { env } from "@/lib/env";

/**
 * Troca da própria senha pelo servidor do Next (BFF): lê o token do cookie `httpOnly` e chama
 * `PATCH /usuarios/me/senha` da API (contrato 2.6; a antiga `/auth/senha` segue até a 3.0).
 *
 * Fica aqui, e não no rewrite `/api/:path*`, porque `/api/auth/*` é do próprio Next (o `proxy.ts` não repassa o
 * token para essas rotas) e porque, depois do 204, **todo token antigo deixa de valer na API**, inclusive o deste
 * cookie: então a sessão é apagada aqui mesmo e a tela manda a pessoa entrar com a senha nova.
 */
export async function PATCH(request: Request) {
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_TOKEN)?.value;
  if (!token) return erro(401, "Unauthorized", "Autenticacao necessaria: entre de novo para trocar a senha");

  const dados = (await request.json().catch(() => ({}))) as Partial<TrocarSenhaRequest>;

  let resposta: Response;
  try {
    resposta = await fetch(`${env.apiUrl}/usuarios/me/senha`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ senhaAtual: dados.senhaAtual ?? "", novaSenha: dados.novaSenha ?? "" }),
      cache: "no-store",
    });
  } catch {
    return erro(503, "Service Unavailable", "Nao foi possivel conectar a API");
  }

  // Erro da API (400, 401, 422, 429…): devolve como veio, no formato padrão que o `apiRequest` já entende.
  if (!resposta.ok) {
    return new Response(await resposta.text(), {
      status: resposta.status,
      headers: { "Content-Type": "application/json" },
    });
  }

  cookieStore.delete(COOKIE_TOKEN);
  cookieStore.delete(COOKIE_SESSAO);
  return new Response(null, { status: 204 });
}

function erro(status: number, error: string, message: string) {
  const corpo: ErroResponse = {
    timestamp: new Date().toISOString(),
    status,
    error,
    message,
    path: "/usuarios/me/senha",
    campos: [],
  };
  return Response.json(corpo, { status });
}
