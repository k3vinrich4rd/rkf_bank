import { afterEach, describe, expect, it, vi } from "vitest";
import type { LoginResponse } from "@/lib/api";
import { COOKIE_REFRESH, COOKIE_SESSAO, COOKIE_TOKEN, apagarSessao, gravarSessao, lerSessao, sessaoDoLogin } from "./cookies";

const LOGIN: LoginResponse = {
  token: "token-de-acesso",
  tipo: "Bearer",
  expiraEmSegundos: 900,
  clienteId: "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
  perfil: "CLIENTE",
  deveTrocarSenha: false,
  refreshToken: "token-de-renovacao",
  refreshExpiraEmSegundos: 604800,
};

function cookiesFalsos() {
  return { set: vi.fn(), delete: vi.fn() };
}

afterEach(() => vi.useRealTimers());

describe("sessaoDoLogin", () => {
  it("guarda o que o navegador pode saber, sem nenhum dos tokens", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-23T12:00:00Z"));

    const sessao = sessaoDoLogin(LOGIN);

    expect(sessao).toEqual({
      clienteId: LOGIN.clienteId,
      perfil: "CLIENTE",
      expiraEm: "2026-10-23T12:15:00.000Z",
      deveTrocarSenha: false,
    });
    expect(JSON.stringify(sessao)).not.toContain("token");
  });
});

describe("gravarSessao", () => {
  it("grava os três cookies httpOnly: acesso pela validade do token, renovação e sessão por 7 dias", () => {
    const cookies = cookiesFalsos();

    gravarSessao(cookies, LOGIN);

    expect(cookies.set).toHaveBeenCalledWith(COOKIE_TOKEN, "token-de-acesso", expect.objectContaining({ httpOnly: true, maxAge: 900 }));
    expect(cookies.set).toHaveBeenCalledWith(
      COOKIE_REFRESH,
      "token-de-renovacao",
      expect.objectContaining({ httpOnly: true, sameSite: "lax", maxAge: 604800 }),
    );
    expect(cookies.set).toHaveBeenCalledWith(COOKIE_SESSAO, expect.any(String), expect.objectContaining({ maxAge: 604800 }));
  });

  it("apagarSessao remove os três", () => {
    const cookies = cookiesFalsos();

    apagarSessao(cookies);

    expect(cookies.delete.mock.calls.map(([nome]) => nome)).toEqual([COOKIE_TOKEN, COOKIE_REFRESH, COOKIE_SESSAO]);
  });
});

describe("lerSessao", () => {
  it("lê a sessão gravada pelo login", () => {
    const valor = JSON.stringify(sessaoDoLogin({ ...LOGIN, deveTrocarSenha: true }));

    expect(lerSessao(valor)).toMatchObject({ perfil: "CLIENTE", deveTrocarSenha: true });
  });

  it("cookie anterior ao contrato 3.1, sem deveTrocarSenha, conta como false", () => {
    const antigo = JSON.stringify({ clienteId: null, perfil: "ADMIN", expiraEm: "2026-10-23T12:15:00.000Z" });

    expect(lerSessao(antigo)).toMatchObject({ perfil: "ADMIN", deveTrocarSenha: false });
  });

  it.each([
    ["ausente", undefined],
    ["JSON quebrado", "{nao-e-json"],
    ["perfil desconhecido", JSON.stringify({ perfil: "ROOT", expiraEm: "2026-10-23T12:15:00.000Z" })],
    ["sem validade", JSON.stringify({ perfil: "CLIENTE" })],
  ])("conteúdo %s vira sem sessão", (_caso, valor) => {
    expect(lerSessao(valor)).toBeNull();
  });
});
