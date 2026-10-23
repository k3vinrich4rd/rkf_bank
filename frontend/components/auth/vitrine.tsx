import type { ReactNode } from "react";

interface VitrineProps {
  /** Rótulo pequeno no topo, em caixa alta. */
  rotulo: string;
  /** O cartão (ou o que a tela quiser mostrar no centro). */
  destaque: ReactNode;
  titulo: ReactNode;
  /** Fatos curtos em mono, separados por fios. Só o que o banco faz de verdade. */
  fatos: readonly string[];
}

/** Painel da direita no login e no cadastro (desktop): fundo com profundidade, o cartão e três fatos. */
export function Vitrine({ rotulo, destaque, titulo, fatos }: VitrineProps) {
  return (
    <aside
      data-vitrine
      className="relative isolate hidden overflow-hidden border-l border-white/[0.06] bg-[#060a14] lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:justify-between lg:gap-10 lg:p-12 xl:p-16"
    >
      <FundoVitrine />

      <p className="flex items-center gap-2.5 text-xs font-medium tracking-[0.25em] text-muted-foreground uppercase">
        <span aria-hidden="true" className="size-1.5 rounded-full bg-brand-cyan shadow-[0_0_12px_rgb(6_182_212/0.8)]" />
        {rotulo}
      </p>

      <div className="mx-auto w-full max-w-sm">{destaque}</div>

      <div>
        <h2 className="max-w-sm text-3xl leading-[1.15] font-semibold tracking-tight text-balance">{titulo}</h2>
        <ul className="mt-8 max-w-sm font-mono text-xs text-muted-foreground">
          {fatos.map((fato) => (
            <li key={fato} className="border-t border-white/[0.08] py-3 last:border-b">
              {fato}
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}

function FundoVitrine() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
      <div className="absolute inset-0 bg-grid [mask-image:radial-gradient(ellipse_at_center,black_15%,transparent_70%)]" />
      <div className="absolute top-[22%] left-1/2 size-[34rem] -translate-x-1/2 rounded-full bg-brand-blue/[0.14] blur-[130px]" />
      <div className="absolute -right-28 -bottom-28 size-[26rem] rounded-full bg-brand-cyan/[0.09] blur-[110px]" />
      {/* Vinheta: escurece as bordas e puxa o olho para o cartão. */}
      <div className="absolute inset-0 bg-[radial-gradient(120%_90%_at_50%_45%,transparent_55%,rgb(0_0_0/0.6))]" />
      <div className="absolute inset-0 bg-grao opacity-[0.07]" />
    </div>
  );
}
