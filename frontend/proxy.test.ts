import { NextRequest } from "next/server";
import { describe, expect, it, vi } from "vitest";
import { COOKIE_REFRESH, COOKIE_SESSAO, COOKIE_TOKEN } from "@/lib/auth/cookies";
import { proxy } from "./proxy";

const PAR_NOVO = {
  token: "acesso-novo",
  tipo: "Bearer",
  expiraEmSegundos: 900,
  clienteId: "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
  perfil: "CLIENTE",
  deveTrocarSenha: false,
  refreshToken: "renovacao-nova",
  refreshExpiraEmSegundos: 604800,
};

function requisicao(caminho: string, cookies: Record<string, string>, metodo = "GET") {
  const cookie = Object.entries(cookies)
    .map(([nome, valor]) => `${nome}=${encodeURIComponent(valor)}`)
    .join("; ");
  return new NextRequest(`http://localhost:3000${caminho}`, { method: metodo, headers: { cookie } });
}

function apiRenova() {
  const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(PAR_NOVO), { status: 200 }));
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

describe("proxy", () => {
  it("chamada /api com token válido segue para a API com Authorization e sem os cookies do site", async () => {
    const fetch = apiRenova();

    const resposta = await proxy(requisicao("/api/contas", { [COOKIE_TOKEN]: "acesso", [COOKIE_REFRESH]: "renovacao" }));

    expect(fetch).not.toHaveBeenCalled();
    expect(resposta.headers.get("x-middleware-request-authorization")).toBe("Bearer acesso");
    expect(resposta.headers.get("x-middleware-request-cookie")).toBeNull();
  });

  it("token de acesso vencido em chamada /api: renova e responde 307 para a mesma URL com o par novo nos cookies", async () => {
    const fetch = apiRenova();

    const resposta = await proxy(requisicao("/api/contas/x/depositos", { [COOKIE_REFRESH]: "renovacao-antiga" }, "POST"));

    expect(fetch).toHaveBeenCalledOnce();
    // O rewrite para a API descarta Set-Cookie: o 307 garante que o navegador guarde o par novo antes de repetir.
    expect(resposta.status).toBe(307);
    expect(resposta.headers.get("location")).toBe("http://localhost:3000/api/contas/x/depositos");
    expect(resposta.cookies.get(COOKIE_TOKEN)?.value).toBe("acesso-novo");
    expect(resposta.cookies.get(COOKIE_REFRESH)?.value).toBe("renovacao-nova");
    expect(resposta.cookies.get(COOKIE_SESSAO)?.value).toContain("CLIENTE");
  });

  it("/api/auth/sessao renova sem redirecionar: a rota do Next já lê o cookie novo", async () => {
    apiRenova();

    const resposta = await proxy(requisicao("/api/auth/sessao", { [COOKIE_REFRESH]: "renovacao-antiga" }));

    expect(resposta.status).toBe(200);
    expect(resposta.cookies.get(COOKIE_TOKEN)?.value).toBe("acesso-novo");
  });

  it("renovação recusada sem token de acesso apaga a sessão que sobrou", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 401 })));

    const resposta = await proxy(requisicao("/api/contas", { [COOKIE_REFRESH]: "revogado" }));

    expect(resposta.cookies.get(COOKIE_REFRESH)?.value).toBe("");
    expect(resposta.cookies.get(COOKIE_TOKEN)?.value).toBe("");
  });

  it("tela /conta sem sessão manda para o login guardando a volta", async () => {
    const resposta = await proxy(requisicao("/conta/extrato?pagina=2", {}));

    expect(resposta.status).toBe(307);
    const destino = new URL(resposta.headers.get("location")!);
    expect(destino.pathname).toBe("/login");
    expect(destino.searchParams.get("voltar")).toBe("/conta/extrato?pagina=2");
  });

  it("login, logout e troca de senha passam direto: as rotas cuidam dos cookies sozinhas", async () => {
    const fetch = apiRenova();

    await proxy(requisicao("/api/auth/login", { [COOKIE_REFRESH]: "renovacao" }, "POST"));

    expect(fetch).not.toHaveBeenCalled();
  });
});
