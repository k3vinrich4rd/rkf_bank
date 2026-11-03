import { cookies } from "next/headers";
import type { LoginRequest, LoginResponse } from "@/lib/api";
import { BackendIndisponivelError, chamarBackend, respostaServidorAcordando } from "@/lib/api/backend";
import { gravarSessao, sessaoDoLogin } from "@/lib/auth/cookies";

/**
 * Login pelo servidor do Next (BFF): repassa CPF e senha para `POST /auth/login` da API e guarda o token de acesso
 * e o de renovação em cookies `httpOnly`. O navegador recebe só a `Sessao`, sem nenhum token.
 *
 * Como arquivo de rota, este handler vence o repasse genérico `app/api/[...caminho]`.
 *
 * Duas tentativas com prazo (`chamarBackend`): o login costuma ser a primeira chamada depois que a API hibernou.
 * Repetir é seguro: no pior caso a API emite um segundo par de tokens e o primeiro vence sozinho.
 */
export async function POST(request: Request) {
  const credenciais = (await request.json().catch(() => ({}))) as Partial<LoginRequest>;

  let resposta: Response;
  try {
    resposta = await chamarBackend(
      "/auth/login",
      {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ cpf: credenciais.cpf ?? "", senha: credenciais.senha ?? "" }),
      },
      { tentativas: 2 },
    );
  } catch (error) {
    if (error instanceof BackendIndisponivelError) return respostaServidorAcordando("/auth/login");
    throw error;
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
