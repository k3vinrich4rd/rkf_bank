import { describe, expect, it, vi } from "vitest";
import { MENSAGEM_SERVIDOR_ACORDANDO } from "./mensagens";
import { ApiError, MENSAGEM_FALHA_DE_SERVIDOR, apiRequest, mensagemDeFalhaDoServidor } from "./client";

describe("mensagemDeFalhaDoServidor", () => {
  it("503 SERVICO_INDISPONIVEL mostra o texto que veio (servidor acordando, ViaCEP fora)", () => {
    const erro = new ApiError(503, MENSAGEM_SERVIDOR_ACORDANDO, [], "SERVICO_INDISPONIVEL");

    expect(mensagemDeFalhaDoServidor(erro)).toBe(MENSAGEM_SERVIDOR_ACORDANDO);
  });

  it("500 ou falha de rede ficam com a mensagem genérica", () => {
    expect(mensagemDeFalhaDoServidor(new ApiError(500, "Internal Server Error"))).toBe(MENSAGEM_FALHA_DE_SERVIDOR);
    expect(mensagemDeFalhaDoServidor(new TypeError("Failed to fetch"))).toBe(MENSAGEM_FALHA_DE_SERVIDOR);
  });
});

describe("apiRequest", () => {
  it("504 do provedor fora do formato padrão vira o aviso de servidor acordando", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("An error occurred", { status: 504 })));

    const erro = await apiRequest("/contas").catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect(erro).toMatchObject({ status: 503, codigo: "SERVICO_INDISPONIVEL", message: MENSAGEM_SERVIDOR_ACORDANDO });
  });
});
