"use client";

import { useEffect, useMemo } from "react";
import { createLogger, descreverErro, type LogContext, type Logger } from "@/lib/logger";

export interface TelaLogger {
  /** O usuário fez algo (enviou formulário, clicou em transferir…). */
  acao(descricao: string, contexto?: LogContext): void;
  /** A ação terminou bem. */
  sucesso(descricao: string, contexto?: LogContext): void;
  /** A ação falhou. Aceita qualquer erro; `ApiError` vira `{ status, erro }`. */
  falha(descricao: string, erro: unknown, contexto?: LogContext): void;
  /** Logger cru da tela, para casos fora do padrão acima. */
  log: Logger;
}

/**
 * Log de uma tela. Registra quando a tela abre e fecha e devolve atalhos
 * para as ações do usuário. Use em Client Components:
 *
 *   const logTela = useLogDeTela("extrato");
 *   logTela.acao("Filtro aplicado", { periodo });
 *
 * Em desenvolvimento o React (StrictMode) monta a tela duas vezes, então
 * "Tela aberta" aparece em dobro no console — em produção sai uma vez só.
 */
export function useLogDeTela(tela: string): TelaLogger {
  const log = useMemo(() => createLogger(`tela:${tela}`), [tela]);

  useEffect(() => {
    log.info("Tela aberta");
    return () => log.debug("Tela fechada");
  }, [log]);

  return useMemo(
    () => ({
      acao: (descricao, contexto) => log.info(descricao, contexto),
      sucesso: (descricao, contexto) => log.info(descricao, contexto),
      falha: (descricao, erro, contexto) => log.warn(descricao, { ...descreverErro(erro), ...contexto }),
      log,
    }),
    [log],
  );
}
