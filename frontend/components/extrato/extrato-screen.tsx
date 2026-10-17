"use client";

import Link from "next/link";
import { ArrowDownToLine, ReceiptText, RotateCw } from "lucide-react";
import { ContaEstado } from "@/components/conta/conta-estado";
import { ContaShell } from "@/components/conta/conta-shell";
import { ContaVoltar } from "@/components/conta/conta-voltar";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { decimalToCents, formatCurrency } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { ExtratoLista } from "./extrato-lista";
import { useExtrato, type ExtratoState } from "./use-extrato";

/** Extrato da conta: saldo no topo e os lançamentos por dia. O estado vive no `useExtrato`. */
export function ExtratoScreen() {
  const estado = useExtrato();

  return (
    <ContaShell>
      <ContaEstado estado={estado.conta}>
        {(conta) => (
          <div className="flex flex-col animate-in fade-in slide-in-from-bottom-2 duration-500">
            <ContaVoltar />
            <h1 className="mt-8 text-3xl font-semibold tracking-tight">Extrato</h1>
            <p className="mt-3 mb-8 text-sm text-muted-foreground">
              Saldo disponível{" "}
              <span className="font-medium text-foreground tabular-nums">
                {formatCurrency(decimalToCents(conta.saldo))}
              </span>
            </p>

            <Lancamentos contaId={conta.id} {...estado} />
          </div>
        )}
      </ContaEstado>
    </ContaShell>
  );
}

function Lancamentos({ contaId, etapa, lancamentos, erro, tentarDeNovo }: ExtratoState & { contaId: string }) {
  switch (etapa) {
    case "carregando":
      return (
        <div role="status" aria-label="Carregando o extrato" className="flex flex-col gap-3">
          <div className="h-4 w-20 animate-pulse rounded bg-white/[0.06]" />
          <div className="h-72 animate-pulse rounded-2xl bg-white/[0.04]" />
        </div>
      );

    case "erro":
      return (
        <div className="flex flex-col gap-4">
          <FormAlert>{erro}</FormAlert>
          <Button type="button" variant="outline" size="xl" onClick={tentarDeNovo}>
            <RotateCw data-icon="inline-start" aria-hidden="true" />
            Tentar de novo
          </Button>
        </div>
      );

    case "vazio":
      return (
        <div className="flex flex-col items-center rounded-2xl border border-dashed border-white/[0.1] px-6 py-12 text-center">
          <ReceiptText className="size-6 text-muted-foreground" aria-hidden="true" />
          <p className="mt-4 font-medium">Nenhuma movimentação ainda</p>
          <p className="mt-1 text-sm text-muted-foreground">Faça um depósito para começar a usar sua conta.</p>
          <Button asChild variant="outline" size="xl" className="mt-6">
            <Link href={ROUTES.depositar}>
              <ArrowDownToLine data-icon="inline-start" aria-hidden="true" />
              Depositar
            </Link>
          </Button>
        </div>
      );

    case "pronto":
      return <ExtratoLista contaId={contaId} lancamentos={lancamentos} />;
  }
}
