import { logAuth } from "@/lib/logger";
import { apiRequest } from "./client";
import type { LoginRequest, Sessao } from "./types";

/**
 * Sessão do usuário. Estas chamadas vão para as rotas `/api/auth/*` do próprio Next (`app/api/auth`),
 * que guardam o token num cookie `httpOnly`: aqui no navegador só existe a `Sessao`, nunca o token.
 */
export const authApi = {
  async login(data: LoginRequest) {
    // CPF e senha são mascarados pelo logger.
    logAuth.info("Entrando", { cpf: data.cpf });
    const sessao = await apiRequest<Sessao>("/auth/login", { method: "POST", body: data });
    logAuth.info("Sessão iniciada", { perfil: sessao.perfil, expiraEm: sessao.expiraEm });
    return sessao;
  },

  async sair() {
    await apiRequest<void>("/auth/logout", { method: "POST" });
    logAuth.info("Sessão encerrada");
  },

  /** Quem está logado. Sem sessão → `ApiError` 401. */
  sessao() {
    return apiRequest<Sessao>("/auth/sessao");
  },
};
