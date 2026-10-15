import { env } from "@/lib/env";
import { logHttp, type LogContext } from "@/lib/logger";
import type { ErroCampo, ErroResponse } from "./types";

/**
 * De onde as chamadas partem:
 * - navegador → proxy `/api` do próprio Next (mesma origem, sem CORS);
 * - servidor → direto no backend.
 */
function baseUrl(): string {
  const isBrowser = typeof window !== "undefined";
  return isBrowser ? `${window.location.origin}/api` : env.apiUrl;
}

/** Erro tipado da API: carrega status HTTP e os erros por campo. */
export class ApiError extends Error {
  readonly status: number;
  readonly campos: ErroCampo[];

  constructor(status: number, message: string, campos: ErroCampo[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.campos = campos;
  }

  /** Mapa `campo → mensagem`, pronto para alimentar um formulário. */
  get fieldErrors(): Record<string, string> {
    return Object.fromEntries(this.campos.map(({ campo, mensagem }) => [campo, mensagem]));
  }
}

/**
 * A falha foi do servidor, não do pedido: sem resposta (rede caiu) ou status 5xx.
 * Com o backend desligado, o proxy `/api` do Next responde 500 — não é erro de rede.
 */
export function isFalhaDeServidor(error: unknown): boolean {
  return !(error instanceof ApiError) || error.status >= 500;
}

type QueryValue =string | number | boolean | undefined;

interface RequestOptions extends Omit<RequestInit, "body"> {
  /** Corpo serializado como JSON. */
  body?: unknown;
  query?: Record<string, QueryValue>;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = new URL(`${baseUrl()}${path}`);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) url.searchParams.set(key, String(value));
  }
  return url.toString();
}

function isErroResponse(value: unknown): value is ErroResponse {
  return typeof value === "object" && value !== null && "status" in value && "message" in value;
}

async function toApiError(response: Response): Promise<ApiError> {
  const payload: unknown = await response.json().catch(() => null);
  if (isErroResponse(payload)) {
    return new ApiError(payload.status, payload.message, payload.campos ?? []);
  }
  return new ApiError(response.status, response.statusText || "Erro inesperado");
}

/**
 * Cliente HTTP único da aplicação. Funciona em Server e Client Components.
 *
 * - Serializa `body` como JSON e define os headers corretos.
 * - Respostas `204 No Content` resolvem `undefined`.
 * - Respostas de erro viram `ApiError` com o payload padrão do backend.
 */
export async function apiRequest<T>(
  path: `/${string}`,
  { body, query, headers, ...init }: RequestOptions = {},
): Promise<T> {
  const method = init.method ?? "GET";
  const requestId = gerarRequestId();
  const inicio = performance.now();
  logHttp.debug(`${method} ${path} →`, { requestId, query, body });

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      ...init,
      headers: {
        Accept: "application/json",
        ...(body !== undefined && { "Content-Type": "application/json" }),
        ...headers,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    // Sem resposta HTTP: backend fora do ar, proxy do Next falhou ou a rede caiu.
    logHttp.error(`${method} ${path} falhou sem resposta (${duracaoMs(inicio)} ms)`, {
      requestId,
      erro: error instanceof Error ? error.message : String(error),
    });
    throw error;
  }

  if (!response.ok) {
    const apiError = await toApiError(response);
    registrarErro(method, path, apiError, { requestId, duracaoMs: duracaoMs(inicio) });
    throw apiError;
  }

  logHttp.info(`${method} ${path} → ${response.status} (${duracaoMs(inicio)} ms)`, { requestId });
  if (response.status === 204) return undefined as T;

  return (await response.json()) as T;
}

/** 4xx é falha esperada (validação/regra de negócio) → warn; 5xx é falha do servidor → error. */
function registrarErro(method: string, path: string, apiError: ApiError, contexto: LogContext) {
  const nivel = apiError.status >= 500 ? "error" : "warn";
  logHttp[nivel](`${method} ${path} → ${apiError.status}: ${apiError.message}`, {
    ...contexto,
    ...(apiError.campos.length > 0 && { campos: apiError.campos }),
  });
}

/** Identificador local da chamada, para casar as linhas de log de uma mesma requisição. */
function gerarRequestId(): string {
  return globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2, 10);
}

function duracaoMs(inicio: number): number {
  return Math.round(performance.now() - inicio);
}
