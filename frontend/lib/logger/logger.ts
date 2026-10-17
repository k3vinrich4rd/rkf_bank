import { env } from "@/lib/env";
import { mascararDadosPessoais } from "./mascara";

/**
 * Núcleo do log: níveis, formato e saída. Não depende de React, então funciona
 * em Server Components, Client Components e código comum de `lib/`.
 *
 * Formato de saída:
 *   [rkf][api:http] POST /clientes → 201 (184 ms) { requestId: "…" }
 */

export type LogLevel = "debug" | "info" | "warn" | "error";

export type LogContext = Record<string, unknown>;

export interface Logger {
  debug(mensagem: string, contexto?: LogContext): void;
  info(mensagem: string, contexto?: LogContext): void;
  warn(mensagem: string, contexto?: LogContext): void;
  error(mensagem: string, contexto?: LogContext): void;
}

const PRIORIDADE: Record<LogLevel, number> = { debug: 10, info: 20, warn: 30, error: 40 };

function escrever(nivel: LogLevel, escopo: string, mensagem: string, contexto?: LogContext) {
  if (PRIORIDADE[nivel] < PRIORIDADE[env.logLevel]) return;

  const linha = `[rkf][${escopo}] ${mensagem}`;
  if (contexto === undefined) {
    console[nivel](linha);
  } else {
    console[nivel](linha, mascararDadosPessoais(contexto));
  }
}

/** Cria um logger identificado por `escopo` (ex.: `"api:http"`, `"tela:cadastro"`). */
export function createLogger(escopo: string): Logger {
  return {
    debug: (mensagem, contexto) => escrever("debug", escopo, mensagem, contexto),
    info: (mensagem, contexto) => escrever("info", escopo, mensagem, contexto),
    warn: (mensagem, contexto) => escrever("warn", escopo, mensagem, contexto),
    error: (mensagem, contexto) => escrever("error", escopo, mensagem, contexto),
  };
}
