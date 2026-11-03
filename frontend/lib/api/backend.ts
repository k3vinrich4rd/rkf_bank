import { env } from "@/lib/env";
import { logHttp } from "@/lib/logger";
import { MENSAGEM_SERVIDOR_ACORDANDO } from "./mensagens";
import type { ErroResponse } from "./types";

/**
 * Chamadas do servidor do Next (BFF) para a API, com prazo por tentativa e nova tentativa curta.
 *
 * Por quê: no plano gratuito do Render a API hiberna depois de 15 min sem uso, e a primeira requisição espera
 * ~1 min ela subir. Sem prazo, a tela fica girando esse tempo todo sem explicar nada (o `fetch` do Node só desiste
 * depois de minutos). Com prazo, o BFF desiste cedo, tenta de novo quando é seguro repetir e, se ainda não deu,
 * responde 503 com uma mensagem que diz o que está acontecendo ("servidor acordando").
 *
 * Só no servidor: lê `API_URL`, que o navegador nunca vê.
 */

/** Prazo de cada tentativa. 12 s cobre com folga uma resposta normal da API e não deixa a tela presa 1 min. */
export const PRAZO_POR_TENTATIVA_MS = 12_000;
/** Espera curta antes de repetir: dá tempo de o provedor terminar de acordar sem segurar a pessoa demais. */
export const ESPERA_ENTRE_TENTATIVAS_MS = 2_000;

export { MENSAGEM_SERVIDOR_ACORDANDO } from "./mensagens";

export type MotivoIndisponivel = "prazo" | "conexao" | "gateway";

/** A API não respondeu: prazo esgotado, conexão recusada ou 502/503/504 do provedor (a API não chegou a atender). */
export class BackendIndisponivelError extends Error {
  readonly motivo: MotivoIndisponivel;

  constructor(motivo: MotivoIndisponivel, options?: { cause?: unknown }) {
    super(`API indisponível (${motivo})`, options);
    this.name = "BackendIndisponivelError";
    this.motivo = motivo;
  }
}

export interface OpcoesBackend {
  /**
   * Total de tentativas (padrão 1). Repita só o que é seguro repetir: uma tentativa que estourou o prazo pode ter
   * chegado à API. Por isso a troca de senha e a renovação (token de uso único: repetir parece reuso e a API
   * derruba todas as sessões) ficam com 1.
   */
  tentativas?: number;
  prazoMs?: number;
  esperaMs?: number;
}

/**
 * 502 e 504 nunca saem da nossa API; 503 sai (ViaCEP fora do ar), mas em JSON no formato padrão. O 502/503/504 do
 * provedor (Render acordando, Vercel sem resposta) vem em HTML ou texto: esse é o caso de "servidor acordando".
 */
export function ehRespostaDoProvedor(resposta: Response): boolean {
  if (resposta.status === 502 || resposta.status === 504) return true;
  return resposta.status === 503 && !(resposta.headers.get("content-type") ?? "").includes("application/json");
}

/** `fetch` para `API_URL + caminho` com prazo (AbortController) e nova tentativa; falha final vira o erro acima. */
export async function chamarBackend(
  caminho: `/${string}`,
  init: RequestInit = {},
  { tentativas = 1, prazoMs = PRAZO_POR_TENTATIVA_MS, esperaMs = ESPERA_ENTRE_TENTATIVAS_MS }: OpcoesBackend = {},
): Promise<Response> {
  let falha = new BackendIndisponivelError("conexao");

  for (let tentativa = 1; tentativa <= tentativas; tentativa++) {
    if (tentativa > 1) await esperar(esperaMs);

    const controle = new AbortController();
    const prazo = setTimeout(() => controle.abort(), prazoMs);
    try {
      const resposta = await fetch(`${env.apiUrl}${caminho}`, { cache: "no-store", ...init, signal: controle.signal });
      if (!ehRespostaDoProvedor(resposta)) return resposta;
      await resposta.body?.cancel();
      falha = new BackendIndisponivelError("gateway");
    } catch (error) {
      falha = new BackendIndisponivelError(controle.signal.aborted ? "prazo" : "conexao", { cause: error });
    } finally {
      clearTimeout(prazo);
    }
  }

  // Só o caminho, sem a query: nada de dado pessoal no log.
  logHttp.warn(`API sem resposta em ${caminho.split("?")[0]} (${falha.motivo}, ${tentativas} tentativa(s))`);
  throw falha;
}

/** Corpo do 503 no formato padrão de erro da API (o `apiRequest` já entende), com o aviso de "servidor acordando". */
export function corpoServidorAcordando(path: string): ErroResponse {
  return {
    timestamp: new Date().toISOString(),
    status: 503,
    error: "Service Unavailable",
    codigo: "SERVICO_INDISPONIVEL",
    message: MENSAGEM_SERVIDOR_ACORDANDO,
    path,
    campos: [],
  };
}

/** Retry-After: quem chama sabe que vale tentar de novo daqui a pouco (a API leva ~1 min para acordar). */
export const HEADERS_SERVIDOR_ACORDANDO = { "Retry-After": "30" } as const;

export function respostaServidorAcordando(path: string): Response {
  return Response.json(corpoServidorAcordando(path), { status: 503, headers: HEADERS_SERVIDOR_ACORDANDO });
}

function esperar(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
