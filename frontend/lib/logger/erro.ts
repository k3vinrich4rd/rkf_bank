import type { LogContext } from "./logger";

/**
 * Resume qualquer erro em um contexto de log curto e seguro.
 *
 * Usa "duck typing" no `status` em vez de importar `ApiError`, para o logger
 * não depender da camada de API (que já depende dele).
 */
export function descreverErro(erro: unknown): LogContext {
  const status = typeof erro === "object" && erro !== null && "status" in erro ? erro.status : "sem-resposta";
  const mensagem = erro instanceof Error ? erro.message : String(erro);
  return { status, erro: mensagem };
}
