import { Nfc } from "lucide-react";
import { LogoMark } from "@/components/brand/logo";
import { cn } from "@/lib/utils";

interface BankCardProps {
  holder?: string;
  lastDigits?: string;
  className?: string;
}

/** Representação visual (decorativa) do cartão RKF. */
export function BankCard({
  holder = "Seu nome aqui",
  lastDigits = "4821",
  className,
}: BankCardProps) {
  return (
    <div
      role="img"
      aria-label="Cartão RKF Banco Digital"
      className={cn(
        "relative aspect-[1.586] w-full overflow-hidden rounded-2xl border border-white/10 bg-[linear-gradient(135deg,#0f1e3d_0%,#0a0f1c_55%,#0b2a4a_100%)] p-5 shadow-2xl shadow-brand-blue/20 sm:p-6",
        className,
      )}
    >
      <CardWaves />

      <div className="relative flex h-full flex-col justify-between">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <LogoMark className="size-6" />
            <span className="leading-none">
              <span className="block text-sm font-bold tracking-[0.2em]">RKF</span>
              <span className="block text-[0.45rem] tracking-[0.3em] text-muted-foreground uppercase">
                Banco Digital
              </span>
            </span>
          </div>
          <Nfc className="size-5 text-muted-foreground" aria-hidden="true" />
        </div>

        <CardChip />

        <div className="flex items-end justify-between gap-4">
          <div className="min-w-0">
            <p className="font-mono text-sm tracking-[0.2em] text-foreground/90">
              •••• {lastDigits}
            </p>
            <p className="mt-1 truncate text-[0.65rem] tracking-[0.2em] text-muted-foreground uppercase">
              {holder}
            </p>
          </div>
          <span className="text-xs font-semibold tracking-[0.2em] text-brand-cyan uppercase">
            Débito
          </span>
        </div>
      </div>
    </div>
  );
}

function CardChip() {
  return (
    <div
      aria-hidden="true"
      className="h-7 w-10 rounded-md border border-amber-200/30 bg-[linear-gradient(135deg,#e2c98a,#a88a4c)] opacity-90"
    />
  );
}

function CardWaves() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 400 252"
      preserveAspectRatio="none"
      className="pointer-events-none absolute inset-0 size-full"
    >
      {[0, 14, 28, 42, 56].map((offset) => (
        <path
          key={offset}
          d={`M-20 ${200 - offset} C 90 ${120 - offset}, 200 ${260 - offset}, 420 ${110 - offset}`}
          stroke="var(--brand-blue)"
          strokeOpacity={0.12 + offset / 300}
          strokeWidth="1.2"
          fill="none"
        />
      ))}
    </svg>
  );
}
