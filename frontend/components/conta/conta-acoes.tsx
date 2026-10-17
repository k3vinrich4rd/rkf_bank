import Link from "next/link";
import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, ReceiptText, type LucideIcon } from "lucide-react";
import { ROUTES } from "@/lib/routes";

interface Acao {
  rotulo: string;
  icone: LucideIcon;
  href: string;
}

const ACOES: readonly Acao[] = [
  { rotulo: "Depositar", icone: ArrowDownToLine, href: ROUTES.depositar },
  { rotulo: "Sacar", icone: ArrowUpFromLine, href: ROUTES.sacar },
  { rotulo: "Transferir", icone: ArrowLeftRight, href: ROUTES.transferir },
  { rotulo: "Extrato", icone: ReceiptText, href: ROUTES.extrato },
];

export function ContaAcoes() {
  return (
    <nav aria-label="Ações da conta">
      <ul className="grid grid-cols-4 gap-3">
        {ACOES.map(({ rotulo, icone: Icone, href }) => (
          <li key={rotulo}>
            <Link
              href={href}
              className="flex flex-col items-center gap-2 rounded-2xl border border-white/[0.08] bg-white/[0.02] px-2 py-4 text-sm font-medium transition-colors hover:border-white/[0.16] hover:bg-white/[0.04]"
            >
              <Icone className="size-5 text-brand-cyan" aria-hidden="true" />
              {rotulo}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
