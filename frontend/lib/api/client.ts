import { env } from "@/lib/env";
import type { ErroCampo, ErroResponse } from "./types";

const API_PREFIX = "/api";

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

type QueryValue = string | number | boolean | undefined;

interface RequestOptions extends Omit<RequestInit, "body"> {
  /** Corpo serializado como JSON. */
  body?: unknown;
  query?: Record<string, QueryValue>;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = new URL(`${API_PREFIX}${path}`, env.apiUrl);
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
  const response = await fetch(buildUrl(path, query), {
    ...init,
    headers: {
      Accept: "application/json",
      ...(body !== undefined && { "Content-Type": "application/json" }),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;

  return (await response.json()) as T;
}
