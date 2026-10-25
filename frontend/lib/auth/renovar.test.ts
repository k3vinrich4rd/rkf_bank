import { afterEach, describe, expect, it, vi } from "vitest";
import { precisaRenovar, renovarSessao, revogarSessao } from "./renovar";

const AGORA = new Date("2026-10-23T12:00:00Z");

afterEach(() => vi.useRealTimers());

describe("precisaRenovar", () => {
  it("sem token de acesso (o cookie some quando vence) precisa renovar", () => {
    expect(precisaRenovar(undefined, undefined)).toBe(true);
  });

  it("com mais de 1 minuto de folga não renova; com menos, renova antes de vencer", () => {
    vi.useFakeTimers();
    vi.setSystemTime(AGORA);

    expect(precisaRenovar("token", "2026-10-23T12:05:00Z")).toBe(false);
    expect(precisaRenovar("token", "2026-10-23T12:00:30Z")).toBe(true);
  });

  it("token sem validade conhecida segue como está (a API decide)", () => {
    expect(precisaRenovar("token", undefined)).toBe(false);
  });
});

describe("renovarSessao", () => {
  it("manda o token de renovação no corpo do POST /auth/refresh e devolve o par novo", async () => {
    const novo = { token: "novo", refreshToken: "renovacao-nova" };
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(novo), { status: 200 }));
    vi.stubGlobal("fetch", fetch);

    await expect(renovarSessao("renovacao-antiga")).resolves.toEqual(novo);

    const [url, opcoes] = fetch.mock.calls[0];
    expect(url).toMatch(/\/auth\/refresh$/);
    expect(opcoes).toMatchObject({ method: "POST", body: JSON.stringify({ refreshToken: "renovacao-antiga" }) });
  });

  it("renovação recusada (vencida, revogada, reuso) devolve null", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 401 })));

    await expect(renovarSessao("revogado")).resolves.toBeNull();
  });

  it("API fora do ar também devolve null, sem estourar", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("fetch failed")));

    await expect(renovarSessao("qualquer")).resolves.toBeNull();
  });
});

describe("revogarSessao", () => {
  it("sem token de renovação não chama a API", async () => {
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);

    await revogarSessao(undefined);

    expect(fetch).not.toHaveBeenCalled();
  });

  it("falha da API não impede sair", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("fetch failed")));

    await expect(revogarSessao("token")).resolves.toBeUndefined();
  });
});
