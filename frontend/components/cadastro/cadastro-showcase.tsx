import { ChartNoAxesColumn, ShieldCheck, Zap } from "lucide-react";
import { BankCard } from "@/components/marketing/bank-card";

const BENEFICIOS = [
  { icon: Zap, texto: "Transferências na hora, 24/7" },
  { icon: ChartNoAxesColumn, texto: "Extrato em tempo real" },
  { icon: ShieldCheck, texto: "Sem mensalidade, sem letra miúda" },
] as const;

interface CadastroShowcaseProps {
  /** Nome digitado no formulário — aparece no cartão em tempo real. */
  nome: string;
}

/** Painel lateral (desktop): vende a conta enquanto a pessoa preenche. */
export function CadastroShowcase({ nome }: CadastroShowcaseProps) {
  return (
    <aside className="isolate hidden overflow-hidden border-l border-white/[0.06] bg-[#070b16] lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:justify-between lg:gap-10 lg:p-12 xl:p-16">
      <ShowcaseBackground />

      <p className="text-xs font-medium tracking-[0.25em] text-muted-foreground uppercase">
        Conta digital gratuita
      </p>

      <div className="mx-auto w-full max-w-sm">
        <div className="relative">
          <div aria-hidden="true" className="absolute inset-x-6 -bottom-8 h-16 rounded-full bg-brand-blue/30 blur-3xl" />
          <BankCard
            holder={nome || "Seu nome aqui"}
            className="relative -rotate-6 shadow-[0_40px_80px_-20px_rgb(0_0_0/0.8)] transition-transform duration-700 ease-out hover:rotate-0"
          />
        </div>
        <p className="mt-12 text-center text-xs text-muted-foreground/70">
          {nome ? "Esse é o seu cartão RKF." : "Digite seu nome e veja seu cartão ganhar vida."}
        </p>
      </div>

      <div>
        <h2 className="max-w-sm text-3xl leading-[1.15] font-semibold tracking-tight text-balance">
          Seu dinheiro, <span className="text-gradient-brand">do seu jeito.</span>
        </h2>
        <ul className="mt-8 flex flex-col gap-3.5">
          {BENEFICIOS.map(({ icon: Icon, texto }) => (
            <li key={texto} className="flex items-center gap-3 text-sm text-muted-foreground">
              <span className="grid size-7 place-items-center rounded-lg border border-white/[0.08] bg-white/[0.03]">
                <Icon className="size-3.5 text-brand-cyan" aria-hidden="true" />
              </span>
              {texto}
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}

function ShowcaseBackground() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
      <div className="absolute inset-0 bg-grid [mask-image:radial-gradient(ellipse_at_center,black_20%,transparent_70%)]" />
      <div className="absolute top-1/4 left-1/2 size-[32rem] -translate-x-1/2 rounded-full bg-brand-blue/[0.12] blur-[120px]" />
      <div className="absolute -right-24 -bottom-24 size-[24rem] rounded-full bg-brand-cyan/[0.08] blur-[100px]" />
    </div>
  );
}
