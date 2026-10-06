import Link from "next/link";
import { UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

/**
 * Atalho de acesso à conta. Hoje aponta para a página placeholder de
 * login; quando o fluxo de autenticação existir, só a rota muda.
 */
export function LoginButton() {
  return (
    <Button
      asChild
      variant="outline"
      size="icon-lg"
      className="rounded-full border-border bg-surface/60 hover:border-brand-blue/60 hover:text-brand-cyan"
    >
      <Link href={ROUTES.login} aria-label="Entrar na sua conta" title="Entrar">
        <UserRound />
      </Link>
    </Button>
  );
}
