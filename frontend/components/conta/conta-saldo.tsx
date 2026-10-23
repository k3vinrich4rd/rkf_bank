import { CopyButton } from "@/components/ui/copy-button";
import type { ContaResponse } from "@/lib/api";
import { decimalToCents, formatCurrency, formatTipoConta } from "@/lib/format";

interface ContaSaldoProps {
  conta: ContaResponse;
}

/** Saldo em destaque, dados da conta e o ID que outra pessoa usa para transferir. */
export function ContaSaldo({ conta }: ContaSaldoProps) {
  return (
    <section
      aria-labelledby="saldo-titulo"
      className="rounded-3xl border border-white/[0.08] bg-gradient-to-b from-brand-blue/[0.08] to-white/[0.01] p-6 sm:p-8"
    >
      <p id="saldo-titulo" className="text-sm text-muted-foreground">
        Saldo disponível
      </p>
      <p className="mt-2 text-4xl font-semibold tracking-tight tabular-nums">
        {formatCurrency(decimalToCents(conta.saldo))}
      </p>

      <dl className="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-sm">
        <Dado rotulo="Tipo" valor={formatTipoConta(conta.tipoConta)} />
        <Dado rotulo="Agência" valor={conta.agencia} />
        <Dado rotulo="Conta" valor={conta.numeroConta} />
      </dl>

      <div className="mt-6 border-t border-white/[0.08] pt-5">
        <p className="text-xs text-muted-foreground">ID da conta · use para receber transferências</p>
        <div className="mt-2 flex items-center gap-3">
          <code className="min-w-0 flex-1 truncate font-mono text-xs text-foreground/80">{conta.id}</code>
          <CopyButton valor={conta.id} rotulo="Copiar ID" />
        </div>
      </div>
    </section>
  );
}

function Dado({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="flex gap-1.5">
      <dt className="text-muted-foreground">{rotulo}</dt>
      <dd className="font-mono tabular-nums">{valor}</dd>
    </div>
  );
}
