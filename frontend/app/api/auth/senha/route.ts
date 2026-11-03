import { cookies } from "next/headers";
import type { ErroResponse, TrocarSenhaRequest } from "@/lib/api";
import { BackendIndisponivelError, chamarBackend, respostaServidorAcordando } from "@/lib/api/backend";
import { COOKIE_TOKEN, apagarSessao } from "@/lib/auth/cookies";

/**
 * Troca da própria senha pelo servidor do Next (BFF): lê o token do cookie `httpOnly` e chama
 * `PATCH /usuarios/me/senha` da API.
 *
 * Fica aqui, e não no repasse genérico `app/api/[...caminho]`, porque `/api/auth/*` é do próprio Next (o `proxy.ts` não repassa o
 * token para essas rotas) e porque, depois do 204, **todo token antigo deixa de valer na API**, inclusive o deste
 * cookie: então a sessão é apagada aqui mesmo e a tela manda a pessoa entrar com a senha nova.
 *
 * Uma tentativa só, com prazo: se o prazo estourar, a troca pode ter chegado à API, e repetir com a senha antiga
 * daria 401 confuso. A pessoa recebe o aviso de "servidor acordando" e tenta de novo.
 */
export async function PATCH(request: Request) {
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_TOKEN)?.value;
  if (!token) return naoAutenticado();

  const dados = (await request.json().catch(() => ({}))) as Partial<TrocarSenhaRequest>;

  let resposta: Response;
  try {
    resposta = await chamarBackend("/usuarios/me/senha", {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ senhaAtual: dados.senhaAtual ?? "", novaSenha: dados.novaSenha ?? "" }),
    });
  } catch (error) {
    if (error instanceof BackendIndisponivelError) return respostaServidorAcordando("/usuarios/me/senha");
    throw error;
  }

  // Erro da API (400, 401, 422, 429…): devolve como veio, no formato padrão que o `apiRequest` já entende.
  if (!resposta.ok) {
    return new Response(await resposta.text(), {
      status: resposta.status,
      headers: { "Content-Type": "application/json" },
    });
  }

  // A troca derruba todos os tokens na API (inclusive o de renovação, pela versão da senha): a sessão acabou.
  apagarSessao(cookieStore);
  return new Response(null, { status: 204 });
}

function naoAutenticado() {
  const corpo: ErroResponse = {
    timestamp: new Date().toISOString(),
    status: 401,
    error: "Unauthorized",
    codigo: "AUTENTICACAO_NECESSARIA",
    message: "Autenticação necessária: entre de novo para trocar a senha",
    path: "/usuarios/me/senha",
    campos: [],
  };
  return Response.json(corpo, { status: 401 });
}
