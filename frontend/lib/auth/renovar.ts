import type { LoginResponse } from "@/lib/api";
import { BackendIndisponivelError, chamarBackend } from "@/lib/api/backend";

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
 *
 * API sem resposta (hibernando no Render, fora do ar) **não** é renovação recusada: rejeita com
 * `BackendIndisponivelError` para o `proxy.ts` manter a sessão e avisar "servidor acordando". Uma tentativa só,
 * com prazo: repetir um token de uso único que talvez já tenha chegado à API pareceria reuso, e a API derrubaria
 * todas as sessões da pessoa.
 */
export async function renovarSessao(refreshToken: string): Promise<LoginResponse | null> {
  const resposta = await chamarBackend("/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  return resposta.ok ? ((await resposta.json()) as LoginResponse) : null;
}

/** `POST /auth/logout` da API: o token de renovação deixa de valer. Falhar aqui não impede sair do site. */
export async function revogarSessao(refreshToken: string | undefined): Promise<void> {
  if (!refreshToken) return;
  try {
    await chamarBackend("/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } catch (error) {
    // O cookie é apagado de qualquer jeito; o token de renovação vence sozinho em 7 dias.
    if (!(error instanceof BackendIndisponivelError)) throw error;
  }
}
