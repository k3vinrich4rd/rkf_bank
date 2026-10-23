import { BankCard } from "@/components/marketing/bank-card";
import { CartaoInterativo } from "./cartao-interativo";

interface CartaoEmVitrineProps {
  titular: string;
  final?: string;
  legenda: string;
}

/** O cartão RKF flutuando na vitrine: halo azul embaixo, sombra longa e inclinação com o cursor. */
export function CartaoEmVitrine({ titular, final, legenda }: CartaoEmVitrineProps) {
  return (
    <>
      <div className="relative">
        <div aria-hidden="true" className="absolute inset-x-8 -bottom-10 h-20 rounded-full bg-brand-blue/35 blur-3xl" />
        <CartaoInterativo className="-rotate-6">
          <BankCard holder={titular} lastDigits={final} className="shadow-[0_50px_90px_-25px_rgb(0_0_0/0.85)]" />
        </CartaoInterativo>
      </div>
      <p className="mt-14 text-center text-xs text-muted-foreground/70">{legenda}</p>
    </>
  );
}
