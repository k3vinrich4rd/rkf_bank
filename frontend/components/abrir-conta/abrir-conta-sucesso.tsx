import Link from "next/link";
import { ArrowRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ContaResponse } from "@/lib/api";
import { decimalToCents, formatCurrency, formatTipoConta } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

interface AbrirContaSucessoProps {
  conta: ContaResponse;
}

export function AbrirContaSucesso({ conta }: AbrirContaSucessoProps) {
  const dados = [
    { rotulo: "Tipo", valor: formatTipoConta(conta.tipoConta) },
    { rotulo: "Agência", valor: conta.agencia },
    { rotulo: "Conta", valor: conta.numeroConta },
    { rotulo: "Saldo", valor: formatCurrency(decimalToCents(conta.saldo)) },
  ];

  return (
    <div role="status" className="animate-in fade-in slide-in-from-bottom-3 duration-500">
      <span className="relative grid size-12 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/30">
        <span aria-hidden="true" className="absolute inset-0 animate-ping rounded-full bg-emerald-500/10 [animation-iteration-count:2]" />
        <Check className="size-5" strokeWidth={2.5} aria-hidden="true" />
      </span>

      <h1 className="mt-8 text-3xl font-semibold tracking-tight">Sua conta está aberta.</h1>
      <p className="mt-3 text-muted-foreground">Guarde os dados abaixo: é com eles que você recebe dinheiro.</p>

      <dl className="mt-8 grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-white/[0.08] bg-white/[0.08]">
        {dados.map(({ rotulo, valor }) => (
          <div key={rotulo} className="bg-background p-5">
            <dt className="text-xs text-muted-foreground">{rotulo}</dt>
            <dd className="mt-1 font-mono text-lg tabular-nums">{valor}</dd>
          </div>
        ))}
      </dl>

      <Button asChild variant="brand" size="xl" className="mt-10 h-[3.25rem] w-full">
        <Link href={ROUTES.conta}>
          Ir para minha conta
          <ArrowRight data-icon="inline-end" aria-hidden="true" />
        </Link>
      </Button>
    </div>
  );
}
