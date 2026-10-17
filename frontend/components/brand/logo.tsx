import { cn } from "@/lib/utils";

interface LogoProps {
  className?: string;
}

/** Logotipo oficial: só o nome "RKF", sem símbolo nem descritor. */
export function Logo({ className }: LogoProps) {
  return (
    <span className={cn("text-xl font-bold tracking-[0.2em]", className)}>RKF</span>
  );
}
