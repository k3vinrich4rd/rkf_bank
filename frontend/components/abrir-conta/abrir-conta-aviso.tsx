import Link from "next/link";
import { ArrowRight, type LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";

interface AbrirContaAvisoProps {
  icone: LucideIcon;
  titulo: string;
  descricao: string;
  acao: { rotulo: string; href: string };
}

/** Estado em que a tela não pode abrir conta (sem cadastro, conta já aberta…) e indica o próximo passo. */
export function AbrirContaAviso({ icone: Icone, titulo, descricao, acao }: AbrirContaAvisoProps) {
  return (
    <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
      <span className="grid size-12 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
        <Icone className="size-5" aria-hidden="true" />
      </span>

      <h1 className="mt-8 text-3xl font-semibold tracking-tight">{titulo}</h1>
      <p className="mt-3 text-muted-foreground">{descricao}</p>

      <Button asChild variant="brand" size="xl" className="mt-10 h-[3.25rem] w-full">
        <Link href={acao.href}>
          {acao.rotulo}
          <ArrowRight data-icon="inline-end" aria-hidden="true" />
        </Link>
      </Button>
    </div>
  );
}
