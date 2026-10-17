import Link from "next/link";
import { ArrowRight, Check, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ClienteResponse } from "@/lib/api";
import { ROUTES } from "@/lib/routes";

interface CadastroSucessoProps {
  cliente: ClienteResponse;
  /** O login automático deu certo: segue direto para a abertura de conta. Senão, para o login. */
  logado: boolean;
}

export function CadastroSucesso({ cliente, logado }: CadastroSucessoProps) {
  const proximoPasso = logado
    ? { rotulo: "Abrir minha conta", href: ROUTES.abrirConta }
    : { rotulo: "Entrar com CPF e senha", href: `${ROUTES.login}?voltar=${encodeURIComponent(ROUTES.abrirConta)}` };

  const { endereco } = cliente;
  const primeiroNome = cliente.nomeCompleto.split(" ")[0];

  return (
    <div role="status" className="animate-in fade-in slide-in-from-bottom-3 duration-500">
      <span className="relative grid size-12 place-items-center rounded-full bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/30">
        <span aria-hidden="true" className="absolute inset-0 animate-ping rounded-full bg-emerald-500/10 [animation-iteration-count:2]" />
        <Check className="size-5" strokeWidth={2.5} aria-hidden="true" />
      </span>

      <h1 className="mt-8 text-3xl font-semibold tracking-tight">
        Bem-vindo ao RKF, {primeiroNome}.
      </h1>
      <p className="mt-3 text-muted-foreground">
        Seu cadastro está pronto. Confirme o endereço que encontramos pelo seu CEP.
      </p>

      <div className="mt-8 flex gap-4 rounded-2xl border border-white/[0.08] bg-white/[0.02] p-5">
        <MapPin className="mt-0.5 size-4 shrink-0 text-brand-cyan" aria-hidden="true" />
        <address className="text-sm leading-relaxed not-italic">
          <span className="font-medium">
            {endereco.logradouro}, {endereco.numero}
            {endereco.complemento && ` · ${endereco.complemento}`}
          </span>
          <br />
          <span className="text-muted-foreground">
            {endereco.bairro} · {endereco.cidade}/{endereco.uf} · {endereco.cep}
          </span>
        </address>
      </div>

      <Button asChild variant="brand" size="xl" className="mt-10 h-[3.25rem] w-full">
        <Link href={proximoPasso.href}>
          {proximoPasso.rotulo}
          <ArrowRight data-icon="inline-end" aria-hidden="true" />
        </Link>
      </Button>
    </div>
  );
}
