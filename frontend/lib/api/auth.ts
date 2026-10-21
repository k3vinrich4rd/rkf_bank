import { logAuth } from "@/lib/logger";
import { apiRequest } from "./client";
import type { LoginRequest, Sessao, TrocarSenhaRequest } from "./types";

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

  /**
   * Troca a própria senha (contrato 2.3). Depois do 204 a sessão acabou: a API derruba todo token antigo e a rota
   * do Next apaga o cookie, então quem chama deve mandar a pessoa para o login.
   */
  async trocarSenha(data: TrocarSenhaRequest) {
    // As senhas são mascaradas pelo logger.
    await apiRequest<void>("/auth/senha", { method: "PATCH", body: data });
    logAuth.info("Senha trocada; sessão encerrada");
  },

  /** Quem está logado. Sem sessão → `ApiError` 401. */
  sessao() {
    return apiRequest<Sessao>("/auth/sessao");
  },
};
