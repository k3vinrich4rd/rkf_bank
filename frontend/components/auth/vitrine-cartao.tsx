import { BankCard } from "@/components/marketing/bank-card";
import { CartaoInterativo } from "./cartao-interativo";

interface VitrineCartaoProps {
  titular: string;
  /** Uma linha discreta no pé do painel. */
  rodape: string;
}

/**
 * Vitrine minimalista: só o cartão, grande, flutuando sob um foco de luz, e uma linha no pé.
 * Sem título nem lista: o formulário ao lado já diz tudo.
 */
export function VitrineCartao({ titular, rodape }: VitrineCartaoProps) {
  return (
    <aside
      data-vitrine
      aria-hidden="true"
      className="relative isolate hidden overflow-hidden border-l border-white/[0.05] bg-[#05080f] lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:items-center lg:justify-center lg:p-16"
    >
      {/* Foco de luz de cima, como num cartão em vitrine de loja. */}
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute top-[-20%] left-1/2 h-[70%] w-[60%] -translate-x-1/2 rounded-full bg-[radial-gradient(closest-side,rgb(59_130_246/0.22),transparent)] blur-2xl" />
        <div className="absolute top-[58%] left-1/2 h-px w-[70%] -translate-x-1/2 bg-gradient-to-r from-transparent via-white/[0.12] to-transparent" />
        <div className="absolute inset-0 bg-[radial-gradient(110%_80%_at_50%_45%,transparent_50%,rgb(0_0_0/0.65))]" />
        <div className="absolute inset-0 bg-grao opacity-[0.06]" />
      </div>

      <div className="relative w-full max-w-[25rem] animate-flutuar">
        <div className="absolute inset-x-10 -bottom-14 h-16 rounded-full bg-brand-blue/30 blur-3xl" />
        <CartaoInterativo className="-rotate-[8deg]">
          <BankCard holder={titular} className="shadow-[0_60px_100px_-30px_rgb(0_0_0/0.9)]" />
        </CartaoInterativo>
      </div>

      <p className="absolute inset-x-0 bottom-10 text-center font-mono text-[0.7rem] tracking-wide text-muted-foreground/60">
        {rodape}
      </p>
    </aside>
  );
}
