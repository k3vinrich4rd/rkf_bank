import type { ReactNode } from "react";
import Link from "next/link";
import { Lock } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { ROUTES } from "@/lib/routes";

interface AuthShellProps {
  /** Link no canto superior direito ("Já tem conta? Entrar"). */
  atalho: { pergunta: string; rotulo: string; href: string };
  /** Painel da direita (desktop): a vitrine com o cartão. */
  vitrine: ReactNode;
  children: ReactNode;
}

/**
 * Moldura do login e do cadastro: formulário à esquerda e vitrine à direita (a partir de `lg`).
 * As duas telas usam a mesma moldura para parecerem da mesma família.
 */
export function AuthShell({ atalho, vitrine, children }: AuthShellProps) {
  return (
    <div className="relative isolate grid flex-1 lg:min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,0.9fr)]">
      <FundoFormulario />

      <section className="flex flex-col px-5 py-6 sm:px-10 lg:px-16 lg:py-8">
        <header className="flex items-center justify-between">
          <Link href={ROUTES.home} aria-label="Voltar para o início" className="rounded-lg">
            <Logo />
          </Link>
          <p className="text-sm text-muted-foreground">
            {atalho.pergunta}{" "}
            <Link href={atalho.href} className="font-medium text-foreground underline-offset-4 hover:underline">
              {atalho.rotulo}
            </Link>
          </p>
        </header>

        <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-14">{children}</main>

        <footer className="flex items-center justify-center gap-2 text-xs text-muted-foreground/70">
          <Lock className="size-3" aria-hidden="true" />
          Seus dados trafegam com criptografia
        </footer>
      </section>

      {vitrine}
    </div>
  );
}

function FundoFormulario() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
      <div className="absolute -top-48 left-[-10%] size-[36rem] rounded-full bg-brand-blue/[0.09] blur-[120px]" />
      <div className="absolute inset-0 bg-grao opacity-[0.05]" />
    </div>
  );
}
