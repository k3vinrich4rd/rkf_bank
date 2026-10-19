"use client";

import { ContaEstado } from "@/components/conta/conta-estado";
import { ContaShell } from "@/components/conta/conta-shell";
import { ContaVoltar } from "@/components/conta/conta-voltar";
import { decimalToCents, formatCurrency } from "@/lib/format";
import { MovimentacaoForm } from "./movimentacao-form";
import { MovimentacaoSucesso } from "./movimentacao-sucesso";
import { MOVIMENTACOES, type TipoMovimentacao } from "./movimentacoes";
import { useMovimentacao } from "./use-movimentacao";

/** Depósito, saque ou transferência: mesma tela, configurada por `tipo`. O estado vive no `useMovimentacao`. */
export function MovimentacaoScreen({ tipo }: { tipo: TipoMovimentacao }) {
  const estado = useMovimentacao(tipo);
  const { titulo, subtitulo, icone: Icone } = MOVIMENTACOES[tipo];

  // O sucesso vem antes do estado da conta: ela é recarregada logo depois da movimentação.
  if (estado.comprovante) {
    return (
      <ContaShell>
        <MovimentacaoSucesso
          tipo={tipo}
          comprovante={estado.comprovante}
          conta={estado.conta.conta}
          onRepetir={estado.novaMovimentacao}
        />
      </ContaShell>
    );
  }

  return (
    <ContaShell>
      <ContaEstado estado={estado.conta}>
        {(conta) => (
          <div className="flex flex-col animate-in fade-in slide-in-from-bottom-2 duration-500">
            <ContaVoltar />

            <span className="mt-8 grid size-12 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
              <Icone className="size-5" aria-hidden="true" />
            </span>
            <h1 className="mt-6 text-3xl font-semibold tracking-tight">{titulo}</h1>
            <p className="mt-3 text-muted-foreground">{subtitulo}</p>
            <p className="mt-6 mb-8 text-sm text-muted-foreground">
              Saldo disponível{" "}
              <span className="font-medium text-foreground tabular-nums">
                {formatCurrency(decimalToCents(conta.saldo))}
              </span>
            </p>

            <MovimentacaoForm {...estado} />
          </div>
        )}
      </ContaEstado>
    </ContaShell>
  );
}
