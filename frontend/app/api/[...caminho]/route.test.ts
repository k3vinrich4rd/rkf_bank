import { describe, expect, it, vi } from "vitest";
import { MENSAGEM_SERVIDOR_ACORDANDO } from "@/lib/api/backend";
import { GET, POST } from "./route";

function contexto(...caminho: string[]) {
  return { params: Promise.resolve({ caminho }) };
}

describe("repasse /api/* → API", () => {
  it("repassa método, caminho, query, corpo e só os headers que a API usa", async () => {
    const fetch = vi.fn().mockResolvedValue(
      Response.json({ id: "1" }, { status: 201, headers: { "Idempotency-Replayed": "false" } }),
    );
    vi.stubGlobal("fetch", fetch);
    const pedido = new Request("http://localhost:3000/api/contas/abc/depositos?origem=site", {
      method: "POST",
      headers: {
        authorization: "Bearer acesso",
        "content-type": "application/json",
        "idempotency-key": "chave-1",
        cookie: "rkf_token=nao-pode-sair",
      },
      body: JSON.stringify({ valor: "10.00" }),
    });

    const resposta = await POST(pedido, contexto("contas", "abc", "depositos"));

    expect(resposta.status).toBe(201);
    expect(resposta.headers.get("idempotency-replayed")).toBe("false");
    const [url, opcoes] = fetch.mock.calls[0] as [string, RequestInit];
    expect(url).toMatch(/\/contas\/abc\/depositos\?origem=site$/);
    expect(opcoes.method).toBe("POST");
    const headers = new Headers(opcoes.headers);
    expect(headers.get("authorization")).toBe("Bearer acesso");
    expect(headers.get("idempotency-key")).toBe("chave-1");
    expect(headers.get("cookie")).toBeNull();
    expect(new TextDecoder().decode(opcoes.body as ArrayBuffer)).toBe(JSON.stringify({ valor: "10.00" }));
  });

  it("API hibernando: GET tenta duas vezes e responde 503 com o aviso de servidor acordando", async () => {
    vi.useFakeTimers();
    const fetch = vi.fn().mockResolvedValue(new Response("Service Unavailable", { status: 503 }));
    vi.stubGlobal("fetch", fetch);

    const chamada = GET(new Request("http://localhost:3000/api/contas"), contexto("contas"));
    await vi.runAllTimersAsync();
    const resposta = await chamada;
    vi.useRealTimers();

    expect(fetch).toHaveBeenCalledTimes(2);
    expect(resposta.status).toBe(503);
    await expect(resposta.json()).resolves.toMatchObject({
      codigo: "SERVICO_INDISPONIVEL",
      message: MENSAGEM_SERVIDOR_ACORDANDO,
      path: "/contas",
    });
  });

  it("POST sem resposta da API não repete (um depósito pode ter chegado): 503 na primeira falha", async () => {
    const fetch = vi.fn().mockRejectedValue(new TypeError("fetch failed"));
    vi.stubGlobal("fetch", fetch);

    const pedido = new Request("http://localhost:3000/api/contas/abc/saques", { method: "POST", body: "{}" });
    const resposta = await POST(pedido, contexto("contas", "abc", "saques"));

    expect(fetch).toHaveBeenCalledOnce();
    expect(resposta.status).toBe(503);
  });
});
