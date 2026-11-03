import { BackendIndisponivelError, chamarBackend, respostaServidorAcordando } from "@/lib/api/backend";

/**
 * Repasse genérico `/api/*` → API (BFF). Substitui o antigo rewrite do `next.config.ts` porque o rewrite não deixa
 * pôr prazo nem trocar a resposta: com a API hibernando no Render (~1 min para acordar), a tela ficava girando sem
 * explicação. Aqui cada chamada tem prazo por tentativa (`chamarBackend`) e, se a API não responder, volta um 503 no
 * formato padrão com a mensagem de "servidor acordando".
 *
 * O `proxy.ts` roda antes e já põe o `Authorization: Bearer` do cookie `httpOnly` (o navegador nunca vê o token).
 * As rotas de `/api/auth/*` são arquivos próprios e vencem este catch-all.
 *
 * Só GET/HEAD repetem: são idempotentes. POST/PATCH/DELETE vão uma vez, porque uma tentativa que estourou o prazo pode
 * ter chegado à API (um depósito repetido sem querer é pior que pedir para tentar de novo).
 */

/** Headers que a API usa; o resto (cookies do site, host, headers do provedor) não sai do Next. */
const HEADERS_PARA_A_API = ["accept", "authorization", "content-type", "idempotency-key", "x-request-id"];

/** Headers que o `fetch` já resolveu (corpo descompactado) ou que são da conexão: não valem para a resposta nova. */
const HEADERS_DA_CONEXAO = ["connection", "content-encoding", "content-length", "keep-alive", "transfer-encoding"];

async function repassar(request: Request, { params }: RouteContext<"/api/[...caminho]">): Promise<Response> {
  const { caminho } = await params;
  const rota = `/${caminho.map(encodeURIComponent).join("/")}` as const;
  const { search } = new URL(request.url);
  const metodo = request.method.toUpperCase();
  const repetivel = metodo === "GET" || metodo === "HEAD";

  const headers = new Headers();
  for (const nome of HEADERS_PARA_A_API) {
    const valor = request.headers.get(nome);
    if (valor) headers.set(nome, valor);
  }

  try {
    const resposta = await chamarBackend(
      `${rota}${search}`,
      {
        method: metodo,
        headers,
        body: repetivel ? undefined : await request.arrayBuffer(),
        redirect: "manual",
      },
      { tentativas: repetivel ? 2 : 1 },
    );

    const headersDaResposta = new Headers(resposta.headers);
    for (const nome of HEADERS_DA_CONEXAO) headersDaResposta.delete(nome);
    return new Response(resposta.body, {
      status: resposta.status,
      statusText: resposta.statusText,
      headers: headersDaResposta,
    });
  } catch (error) {
    if (error instanceof BackendIndisponivelError) return respostaServidorAcordando(rota);
    throw error;
  }
}

export { repassar as DELETE, repassar as GET, repassar as HEAD, repassar as PATCH, repassar as POST, repassar as PUT };
