import type { ReactNode } from "react";
import Link from "next/link";
import { Lock } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { ROUTES } from "@/lib/routes";

/** Moldura das telas da conta: logo no topo, conteúdo centralizado e aviso de segurança no rodapé. */
export function ContaShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-1 flex-col px-5 py-6 sm:px-10 lg:py-8">
      <header>
        <Link href={ROUTES.home} aria-label="Voltar para o início" className="inline-flex rounded-lg">
          <Logo />
        </Link>
      </header>

      <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-14">{children}</main>

      <footer className="flex items-center justify-center gap-2 text-xs text-muted-foreground/70">
        <Lock className="size-3" aria-hidden="true" />
        Seus dados trafegam com criptografia
      </footer>
    </div>
  );
}
