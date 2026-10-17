import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { ROUTES } from "@/lib/routes";

/** Link "Voltar para a conta", no topo das telas de movimentação e do extrato. */
export function ContaVoltar() {
  return (
    <Link
      href={ROUTES.conta}
      className="inline-flex items-center gap-1.5 self-start rounded-lg text-sm text-muted-foreground transition-colors hover:text-foreground"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Sua conta
    </Link>
  );
}
