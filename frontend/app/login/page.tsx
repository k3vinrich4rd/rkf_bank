import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft, LockKeyhole } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Entrar",
  robots: { index: false, follow: false },
};

/**
 * Placeholder da área de login.
 *
 * TODO(auth): substituir pelo formulário real quando o backend expuser
 * autenticação (ver "Login" em planing/decisoes.md).
 */
export default function LoginPage() {
  return (
    <main className="relative isolate grid flex-1 place-items-center px-4 py-16">
      <div
        aria-hidden="true"
        className="absolute top-1/4 left-1/2 -z-10 size-[30rem] -translate-x-1/2 rounded-full bg-brand-blue/15 blur-[120px]"
      />

      <div className="w-full max-w-sm rounded-3xl border border-border bg-surface/80 p-8 text-center backdrop-blur">
        <Link href={ROUTES.home} className="inline-flex" aria-label="Voltar para o início">
          <Logo />
        </Link>

        <span className="mx-auto mt-8 grid size-14 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
          <LockKeyhole className="size-6" aria-hidden="true" />
        </span>

        <h1 className="mt-6 text-2xl font-semibold tracking-tight">Área do cliente</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          O acesso à sua conta estará disponível em breve.
        </p>

        <Button asChild variant="outline" size="xl" className="mt-8 w-full">
          <Link href={ROUTES.home}>
            <ArrowLeft data-icon="inline-start" />
            Voltar para o site
          </Link>
        </Button>
      </div>
    </main>
  );
}
