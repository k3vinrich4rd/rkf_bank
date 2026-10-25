import type { LoginResponse } from "@/lib/api";
import { env } from "@/lib/env";

/** Renova a sessão quando faltar menos que isto para o token de acesso vencer (a requisição não chega vencida). */
const MARGEM_MS = 60_000;

/**
 * Precisa renovar? Sem token de acesso (o cookie some quando ele vence) ou perto de vencer pela `Sessao`.
 * Só roda no servidor do Next (`proxy.ts`): o navegador nunca vê nenhum dos dois tokens.
 */
export function precisaRenovar(tokenDeAcesso: string | undefined, expiraEm: string | undefined): boolean {
  if (!tokenDeAcesso) return true;
  if (!expiraEm) return false;
  return new Date(expiraEm).getTime() - Date.now() < MARGEM_MS;
}

/**
 * `POST /auth/refresh` da API (contrato 3.1). Devolve o par novo, ou `null` quando a renovação não vale mais
 * (vencida, revogada no logout, senha trocada, reuso detectado): aí a sessão acabou e a pessoa entra de novo.
 * Cada token de renovação é de uso único: o par novo substitui o anterior nos cookies.
 */
export async function renovarSessao(refreshToken: string): Promise<LoginResponse | null> {
  try {
    const resposta = await fetch(`${env.apiUrl}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    return resposta.ok ? ((await resposta.json()) as LoginResponse) : null;
  } catch {
    // API fora do ar: não derruba a sessão por isso; a próxima requisição tenta de novo.
    return null;
  }
}

/** `POST /auth/logout` da API: o token de renovação deixa de valer. Falhar aqui não impede sair do site. */
export async function revogarSessao(refreshToken: string | undefined): Promise<void> {
  if (!refreshToken) return;
  try {
    await fetch(`${env.apiUrl}/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
  } catch {
    // O cookie é apagado de qualquer jeito; o token de renovação vence sozinho em 7 dias.
  }
}
