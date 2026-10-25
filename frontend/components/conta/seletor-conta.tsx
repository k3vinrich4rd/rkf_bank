import type { ContaResponse } from "@/lib/api";
import { formatTipoConta } from "@/lib/format";

interface SeletorContaProps {
  contas: ContaResponse[];
  contaId: string;
  onEscolher: (contaId: string) => void;
}

/**
 * Troca entre as contas do cliente (contrato 4.1: corrente e poupança, por exemplo). Só aparece com duas ou mais;
 * com uma conta não há o que escolher. A escolha vale para todas as telas da conta (`useConta`).
 */
export function SeletorConta({ contas, contaId, onEscolher }: SeletorContaProps) {
  if (contas.length < 2) return null;

  return (
    <div role="radiogroup" aria-label="Conta" className="mb-4 flex flex-wrap gap-2">
      {contas.map((conta) => {
        const ativa = conta.id === contaId;
        return (
          <button
            key={conta.id}
            type="button"
            role="radio"
            aria-checked={ativa}
            onClick={() => onEscolher(conta.id)}
            className={`flex flex-col rounded-xl border px-3.5 py-2 text-left transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-blue ${
              ativa
                ? "border-brand-blue/60 bg-brand-blue/10 text-foreground"
                : "border-white/[0.08] text-muted-foreground hover:border-white/20 hover:text-foreground"
            }`}
          >
            <span className="text-sm font-medium">{formatTipoConta(conta.tipoConta)}</span>
            <span className="font-mono text-[0.7rem]">
              Ag. {conta.agencia} · {conta.numeroConta}
            </span>
          </button>
        );
      })}
    </div>
  );
}
