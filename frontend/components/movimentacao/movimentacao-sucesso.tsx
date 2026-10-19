import type { ReactNode } from "react";
import Link from "next/link";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ContaResponse } from "@/lib/api";
import { decimalToCents, formatCurrency } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { MOVIMENTACOES, type TipoMovimentacao } from "./movimentacoes";
import type { Comprovante } from "./use-movimentacao";

interface MovimentacaoSucessoProps {
  tipo: TipoMovimentacao;
  comprovante: Comprovante;
  /** Conta recarregada depois da movimentação; `null` enquanto o saldo novo não chega. */
  conta: ContaResponse | null;
  onRepetir: () => void;
}

export function MovimentacaoSucesso({ tipo, comprovante, conta, onRepetir }: MovimentacaoSucessoProps) {
  const config = MOVIMENTACOES[tipo];

  return (
    <div role="status" className="animate-in fade-in slide-in-from-bottom-3 duration-500">
      <span className="relative grid size-12 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/30">
        <span aria-hidden="true" className="absolute inset-0 animate-ping rounded-full bg-emerald-500/10 [animation-iteration-count:2]" />
        <Check className="size-5" strokeWidth={2.5} aria-hidden="true" />
      </span>

      <h1 className="mt-8 text-3xl font-semibold tracking-tight">{config.tituloSucesso}</h1>
      <p className="mt-3 text-4xl font-semibold tracking-tight tabular-nums">{formatCurrency(comprovante.valor)}</p>

      <dl className="mt-8 flex flex-col gap-px overflow-hidden rounded-2xl border border-white/[0.08] bg-white/[0.08]">
        {comprovante.contaDestinoId && (
          <Linha rotulo="Para a conta">
            <span className="block truncate font-mono text-sm">{comprovante.contaDestinoId}</span>
          </Linha>
        )}
        {comprovante.descricao && <Linha rotulo="Descrição">{comprovante.descricao}</Linha>}
        <Linha rotulo="Saldo atual">
          {conta ? (
            <span className="font-mono tabular-nums">{formatCurrency(decimalToCents(conta.saldo))}</span>
          ) : (
            <span className="block h-6 w-28 animate-pulse rounded-md bg-white/[0.06]" aria-label="Atualizando saldo" />
          )}
        </Linha>
      </dl>

      <div className="mt-10 flex flex-col gap-3">
        <Button asChild variant="brand" size="xl" className="h-[3.25rem] w-full">
          <Link href={ROUTES.conta}>Voltar para a conta</Link>
        </Button>
        <Button type="button" variant="outline" size="xl" onClick={onRepetir}>
          {config.rotuloRepetir}
        </Button>
      </div>
    </div>
  );
}

function Linha({ rotulo, children }: { rotulo: string; children: ReactNode }) {
  return (
    <div className="bg-background p-5">
      <dt className="text-xs text-muted-foreground">{rotulo}</dt>
      <dd className="mt-1">{children}</dd>
    </div>
  );
}
