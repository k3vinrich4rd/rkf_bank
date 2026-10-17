import Link from "next/link";
import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, ReceiptText, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface Acao {
  rotulo: string;
  icone: LucideIcon;
  /** Sem `href`, a tela ainda não existe e o botão aparece como "Em breve". */
  href?: string;
}

// TODO(movimentacoes): preencher `href` (via ROUTES) conforme cada tela for criada.
const ACOES: readonly Acao[] = [
  { rotulo: "Depositar", icone: ArrowDownToLine },
  { rotulo: "Sacar", icone: ArrowUpFromLine },
  { rotulo: "Transferir", icone: ArrowLeftRight },
  { rotulo: "Extrato", icone: ReceiptText },
];

const ESTILO_ACAO =
  "flex flex-col items-center gap-2 rounded-2xl border border-white/[0.08] bg-white/[0.02] px-2 py-4 text-sm font-medium transition-colors";

export function ContaAcoes() {
  return (
    <nav aria-label="Ações da conta">
      <ul className="grid grid-cols-4 gap-3">
        {ACOES.map((acao) => (
          <li key={acao.rotulo}>
            <BotaoAcao {...acao} />
          </li>
        ))}
      </ul>
    </nav>
  );
}

function BotaoAcao({ rotulo, icone: Icone, href }: Acao) {
  const conteudo = (
    <>
      <Icone className="size-5 text-brand-cyan" aria-hidden="true" />
      {rotulo}
    </>
  );

  if (!href) {
    return (
      <span aria-disabled="true" title="Em breve" className={cn(ESTILO_ACAO, "cursor-not-allowed opacity-50")}>
        {conteudo}
        <span className="sr-only">(em breve)</span>
      </span>
    );
  }

  return (
    <Link href={href} className={cn(ESTILO_ACAO, "hover:border-white/[0.16] hover:bg-white/[0.04]")}>
      {conteudo}
    </Link>
  );
}
