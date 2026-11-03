import { afterEach, describe, expect, it, vi } from "vitest";
import {
  BackendIndisponivelError,
  MENSAGEM_SERVIDOR_ACORDANDO,
  PRAZO_POR_TENTATIVA_MS,
  chamarBackend,
  respostaServidorAcordando,
} from "./backend";

afterEach(() => vi.useRealTimers());

/** `fetch` que só termina quando alguém aborta: a API hibernando no Render, que não responde a tempo. */
function fetchQueNaoResponde() {
  return vi.fn((_url: string, init?: RequestInit) => {
    return new Promise<Response>((_resolve, reject) => {
      init?.signal?.addEventListener("abort", () => reject(new DOMException("aborted", "AbortError")));
    });
  });
}

describe("chamarBackend", () => {
  it("resposta normal da API passa direto, na primeira tentativa", async () => {
    const fetch = vi.fn().mockResolvedValue(Response.json({ ok: true }));
    vi.stubGlobal("fetch", fetch);

    const resposta = await chamarBackend("/contas", {}, { tentativas: 2 });

    expect(resposta.status).toBe(200);
    expect(fetch).toHaveBeenCalledOnce();
    expect(fetch.mock.calls[0][0]).toMatch(/\/contas$/);
    expect(fetch.mock.calls[0][1].signal).toBeInstanceOf(AbortSignal);
  });

  it("aborta a tentativa que passa do prazo e, sem tentativa sobrando, rejeita com motivo 'prazo'", async () => {
    vi.useFakeTimers();
    const fetch = fetchQueNaoResponde();
    vi.stubGlobal("fetch", fetch);

    const chamada = chamarBackend("/contas");
    const verificacao = expect(chamada).rejects.toMatchObject({ name: "BackendIndisponivelError", motivo: "prazo" });
    await vi.advanceTimersByTimeAsync(PRAZO_POR_TENTATIVA_MS);
    await verificacao;

    expect(fetch).toHaveBeenCalledOnce();
    expect((fetch.mock.calls[0][1] as RequestInit).signal?.aborted).toBe(true);
  });

  it("502/504 do provedor (API acordando) tenta de novo depois da espera curta", async () => {
    vi.useFakeTimers();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(new Response("<html>Bad Gateway</html>", { status: 502 }))
      .mockResolvedValueOnce(Response.json({ ok: true }));
    vi.stubGlobal("fetch", fetch);

    const chamada = chamarBackend("/contas", {}, { tentativas: 2, esperaMs: 2_000 });
    await vi.advanceTimersByTimeAsync(2_000);

    await expect(chamada).resolves.toMatchObject({ status: 200 });
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it("conexão recusada nas duas tentativas vira BackendIndisponivelError 'conexao'", async () => {
    vi.useFakeTimers();
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("fetch failed")));

    const chamada = chamarBackend("/contas", {}, { tentativas: 2, esperaMs: 10 });
    const verificacao = expect(chamada).rejects.toMatchObject({ motivo: "conexao" });
    await vi.advanceTimersByTimeAsync(10);
    await verificacao;
  });

  it("503 em JSON é da própria API (ViaCEP fora): volta como veio, sem repetir", async () => {
    const corpo = { status: 503, codigo: "SERVICO_INDISPONIVEL", message: "ViaCEP indisponível" };
    const fetch = vi.fn().mockResolvedValue(Response.json(corpo, { status: 503 }));
    vi.stubGlobal("fetch", fetch);

    const resposta = await chamarBackend("/clientes", { method: "POST" }, { tentativas: 2 });

    expect(resposta.status).toBe(503);
    await expect(resposta.json()).resolves.toMatchObject({ message: "ViaCEP indisponível" });
    expect(fetch).toHaveBeenCalledOnce();
  });
});

describe("respostaServidorAcordando", () => {
  it("é um 503 no formato padrão de erro, com o aviso de servidor acordando e Retry-After", async () => {
    const resposta = respostaServidorAcordando("/contas");

    expect(resposta.status).toBe(503);
    expect(resposta.headers.get("retry-after")).toBe("30");
    await expect(resposta.json()).resolves.toMatchObject({
      status: 503,
      codigo: "SERVICO_INDISPONIVEL",
      message: MENSAGEM_SERVIDOR_ACORDANDO,
      path: "/contas",
      campos: [],
    });
  });

  it("o erro carrega o motivo para o log", () => {
    expect(new BackendIndisponivelError("gateway").motivo).toBe("gateway");
  });
});
