"use client";

import { useCallback, useSyncExternalStore } from "react";
import { lerContaAtual, observarContaAtual, salvarContaAtual, type ContaAtual } from "@/lib/conta-atual";

/**
 * Conta guardada no navegador (`lib/conta-atual.ts`), sempre atualizada.
 *
 * `carregando` é `true` na renderização do servidor e na hidratação, quando
 * ainda não dá para ler o `localStorage`. Espere ele virar `false` antes de
 * concluir que o usuário não tem cadastro.
 */
export function useContaAtual() {
  const conta = useSyncExternalStore<ContaAtual | null | undefined>(
    observarContaAtual,
    lerContaAtual,
    () => undefined,
  );

  const salvar = useCallback((nova: ContaAtual | null) => salvarContaAtual(nova), []);

  return {
    conta: conta ?? null,
    carregando: conta === undefined,
    salvar,
  };
}
