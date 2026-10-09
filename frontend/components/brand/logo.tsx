import { useId } from "react";
import { cn } from "@/lib/utils";

interface LogoMarkProps {
  className?: string;
}

/** Símbolo RKF: duas lâminas em gradiente formando uma seta de avanço. */
export function LogoMark({ className }: LogoMarkProps) {
  const gradientId = useId();

  return (
    <svg
      viewBox="0 0 32 32"
      fill="none"
      aria-hidden="true"
      className={cn("size-8", className)}
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="32" x2="32" y2="0">
          <stop offset="0" stopColor="var(--brand-blue)" />
          <stop offset="1" stopColor="var(--brand-cyan)" />
        </linearGradient>
      </defs>
      <path d="M4 4h8v24H4z" fill={`url(#${gradientId})`} />
      <path d="M14 16 26 4h6L20 16z" fill={`url(#${gradientId})`} />
      <path d="M14 16h6l8 12h-6z" fill={`url(#${gradientId})`} opacity=".7" />
    </svg>
  );
}

interface LogoProps {
  className?: string;
  /** Exibe o descritor "Banco Digital" abaixo do nome. */
  withDescriptor?: boolean;
}

export function Logo({ className, withDescriptor = true }: LogoProps) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <LogoMark />
      <span className="flex flex-col leading-none">
        <span className="text-xl font-bold tracking-[0.18em]">RKF</span>
        {withDescriptor && (
          <span className="mt-1 text-[0.55rem] font-medium tracking-[0.3em] text-muted-foreground uppercase">
            Banco Digital
          </span>
        )}
      </span>
    </span>
  );
}
