import { cookies } from "next/headers";
import type { ErroResponse, LoginRequest, LoginResponse, Sessao } from "@/lib/api";
import { COOKIE_SESSAO, COOKIE_TOKEN, opcoesDoCookie } from "@/lib/auth/cookies";
import { env } from "@/lib/env";

/**
 * Login pelo servidor do Next (BFF): repassa CPF e senha para `POST /auth/login` da API e guarda o token
 * num cookie `httpOnly`. O navegador recebe só a `Sessao`, sem o token.
 *
 * Como arquivo de rota, este handler vence o rewrite `/api/:path*` do `next.config.ts`.
 */
export async function POST(request: Request) {
  const credenciais = (await request.json().catch(() => ({}))) as Partial<LoginRequest>;

  let resposta: Response;
  try {
    resposta = await fetch(`${env.apiUrl}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ cpf: credenciais.cpf ?? "", senha: credenciais.senha ?? "" }),
      cache: "no-store",
    });
  } catch {
    return erro(503, "Nao foi possivel conectar a API");
  }

  // Erro da API (400, 401…): devolve como veio, no formato padrão que o `apiRequest` já entende.
  if (!resposta.ok) {
    return new Response(await resposta.text(), {
      status: resposta.status,
      headers: { "Content-Type": "application/json" },
    });
  }

  const login = (await resposta.json()) as LoginResponse;
  const sessao: Sessao = {
    clienteId: login.clienteId,
    perfil: login.perfil,
    expiraEm: new Date(Date.now() + login.expiraEmSegundos * 1000).toISOString(),
  };

  const cookieStore = await cookies();
  const opcoes = opcoesDoCookie(login.expiraEmSegundos);
  cookieStore.set(COOKIE_TOKEN, login.token, opcoes);
  cookieStore.set(COOKIE_SESSAO, JSON.stringify(sessao), opcoes);

  return Response.json(sessao);
}

function erro(status: number, message: string) {
  const corpo: ErroResponse = {
    timestamp: new Date().toISOString(),
    status,
    error: "Service Unavailable",
    codigo: "SERVICO_INDISPONIVEL",
    message,
    path: "/auth/login",
    campos: [],
  };
  return Response.json(corpo, { status });
}
