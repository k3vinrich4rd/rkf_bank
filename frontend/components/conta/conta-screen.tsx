"use client";

import Link from "next/link";
import { KeyRound } from "lucide-react";
import { useLogDeTela } from "@/components/logs";
import { ROUTES } from "@/lib/routes";
import { ContaAcoes } from "./conta-acoes";
import { ContaEstado } from "./conta-estado";
import { ContaSaldo } from "./conta-saldo";
import { ContaShell } from "./conta-shell";
import { useConta } from "./use-conta";

/** Área da conta: saldo, dados e atalhos para as movimentações. O estado vive no `useConta`. */
export function ContaScreen() {
  const logTela = useLogDeTela("conta");
  const estado = useConta(logTela);

  return (
    <ContaShell>
      <ContaEstado estado={estado} esqueleto={<ContaCarregando />}>
        {(conta) => (
          <div className="flex flex-col gap-6 animate-in fade-in slide-in-from-bottom-2 duration-500">
            <h1 className="text-3xl font-semibold tracking-tight">Sua conta</h1>
            <ContaSaldo conta={conta} />
            <ContaAcoes />
            <Link
              href={ROUTES.trocarSenha}
              className="inline-flex items-center gap-2 self-center rounded-lg px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              <KeyRound className="size-4" aria-hidden="true" />
              Trocar senha
            </Link>
          </div>
        )}
      </ContaEstado>
    </ContaShell>
  );
}

/** Esqueleto no formato da tela pronta, para o conteúdo não "pular" quando a conta chega. */
function ContaCarregando() {
  return (
    <div role="status" aria-label="Carregando sua conta" className="flex flex-col gap-6">
      <div className="h-9 w-40 animate-pulse rounded-lg bg-white/[0.06]" />
      <div className="h-64 animate-pulse rounded-3xl bg-white/[0.04]" />
      <div className="grid grid-cols-4 gap-3">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="h-20 animate-pulse rounded-2xl bg-white/[0.04]" />
        ))}
      </div>
    </div>
  );
}
