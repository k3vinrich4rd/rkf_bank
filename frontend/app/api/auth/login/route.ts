import { cookies } from "next/headers";
import type { ErroResponse, LoginRequest, LoginResponse } from "@/lib/api";
import { gravarSessao, sessaoDoLogin } from "@/lib/auth/cookies";
import { env } from "@/lib/env";

/**
 * Login pelo servidor do Next (BFF): repassa CPF e senha para `POST /auth/login` da API e guarda o token de acesso
 * e o de renovação em cookies `httpOnly`. O navegador recebe só a `Sessao`, sem nenhum token.
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
    return erro(503, "Não foi possível conectar à API");
  }

  // Erro da API (400, 401…): devolve como veio, no formato padrão que o `apiRequest` já entende.
  if (!resposta.ok) {
    return new Response(await resposta.text(), {
      status: resposta.status,
      headers: { "Content-Type": "application/json" },
    });
  }

  const login = (await resposta.json()) as LoginResponse;
  gravarSessao(await cookies(), login);
  return Response.json(sessaoDoLogin(login));
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
