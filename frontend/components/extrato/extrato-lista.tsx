import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, type LucideIcon } from "lucide-react";
import type { LancamentoResponse, TipoLancamento, Uuid } from "@/lib/api";
import { decimalToCents, formatCurrency, formatDia, formatHora } from "@/lib/format";
import { cn } from "@/lib/utils";

interface ExtratoListaProps {
  contaId: Uuid;
  lancamentos: LancamentoResponse[];
}

/** Lançamentos agrupados por dia, na ordem da API (do mais recente para o mais antigo). */
export function ExtratoLista({ contaId, lancamentos }: ExtratoListaProps) {
  return (
    <div className="flex flex-col gap-8">
      {agruparPorDia(lancamentos).map(({ dia, itens }) => (
        <section key={dia} aria-label={dia}>
          <h2 className="text-xs font-medium tracking-wide text-muted-foreground uppercase">{dia}</h2>
          <ul className="mt-3 flex flex-col divide-y divide-white/[0.06] rounded-2xl border border-white/[0.08] bg-white/[0.02]">
            {itens.map((lancamento) => (
              <li key={lancamento.id}>
                <LinhaLancamento lancamento={lancamento} contaId={contaId} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

const ICONES: Record<TipoLancamento, LucideIcon> = {
  DEPOSITO: ArrowDownToLine,
  SAQUE: ArrowUpFromLine,
  TRANSFERENCIA: ArrowLeftRight,
};

function LinhaLancamento({ lancamento, contaId }: { lancamento: LancamentoResponse; contaId: Uuid }) {
  // Regra do contrato (4.5): o valor vem sempre positivo; é entrada quando o destino é esta conta.
  const entrada = lancamento.contaDestinoId === contaId;
  const Icone = ICONES[lancamento.tipoLancamento];
  const titulo = tituloDoLancamento(lancamento.tipoLancamento, entrada);
  const valor = formatCurrency(decimalToCents(lancamento.valor));

  return (
    <div className="flex items-center gap-4 px-4 py-4">
      <span
        className={cn(
          "grid size-10 shrink-0 place-items-center rounded-xl border",
          entrada ? "border-emerald-500/25 text-emerald-400" : "border-white/[0.08] text-muted-foreground",
        )}
      >
        <Icone className="size-4" aria-hidden="true" />
      </span>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{titulo}</p>
        <p className="truncate text-xs text-muted-foreground">
          {formatHora(lancamento.dataHora)}
          {lancamento.descricao && ` · ${lancamento.descricao}`}
        </p>
      </div>

      <p className={cn("shrink-0 text-sm font-medium tabular-nums", entrada ? "text-emerald-400" : "text-foreground")}>
        <span className="sr-only">{entrada ? "Entrada de" : "Saída de"} </span>
        <span aria-hidden="true">{entrada ? "+ " : "− "}</span>
        {valor}
      </p>
    </div>
  );
}

function tituloDoLancamento(tipo: TipoLancamento, entrada: boolean): string {
  switch (tipo) {
    case "DEPOSITO":
      return "Depósito";
    case "SAQUE":
      return "Saque";
    case "TRANSFERENCIA":
      return entrada ? "Transferência recebida" : "Transferência enviada";
  }
}

interface GrupoDoDia {
  dia: string;
  itens: LancamentoResponse[];
}

/** Como a lista já vem ordenada por data, basta abrir um grupo novo quando o dia muda. */
function agruparPorDia(lancamentos: LancamentoResponse[]): GrupoDoDia[] {
  const agora = new Date();
  const grupos: GrupoDoDia[] = [];

  for (const lancamento of lancamentos) {
    const dia = formatDia(new Date(lancamento.dataHora), agora);
    const ultimo = grupos.at(-1);
    if (ultimo?.dia === dia) ultimo.itens.push(lancamento);
    else grupos.push({ dia, itens: [lancamento] });
  }

  return grupos;
}
