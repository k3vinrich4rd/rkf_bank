"use client";

import Link from "next/link";
import { LoaderCircle, Lock, UserRound, WalletCards } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { ROUTES } from "@/lib/routes";
import { AbrirContaAviso } from "./abrir-conta-aviso";
import { AbrirContaForm } from "./abrir-conta-form";
import { AbrirContaSucesso } from "./abrir-conta-sucesso";
import { useAbrirConta, type AbrirContaState } from "./use-abrir-conta";

/** Abertura de conta: passo seguinte ao cadastro. O estado vive no `useAbrirConta`. */
export function AbrirContaScreen() {
  const estado = useAbrirConta();

  return (
    <div className="flex flex-1 flex-col px-5 py-6 sm:px-10 lg:py-8">
      <header>
        <Link href={ROUTES.home} aria-label="Voltar para o início" className="inline-flex rounded-lg">
          <Logo />
        </Link>
      </header>

      <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-14">
        <Conteudo {...estado} />
      </main>

      <footer className="flex items-center justify-center gap-2 text-xs text-muted-foreground/70">
        <Lock className="size-3" aria-hidden="true" />
        Seus dados trafegam com criptografia
      </footer>
    </div>
  );
}

function Conteudo(estado: AbrirContaState) {
  switch (estado.etapa) {
    case "carregando":
      return (
        <LoaderCircle className="mx-auto size-6 animate-spin text-muted-foreground" aria-label="Carregando" />
      );

    case "sem-cadastro":
      return (
        <AbrirContaAviso
          icone={UserRound}
          titulo="Primeiro, seu cadastro"
          descricao="Não encontramos seu cadastro neste navegador. Leva menos de 2 minutos."
          acao={{ rotulo: "Fazer meu cadastro", href: ROUTES.signUp }}
        />
      );

    case "ja-tem-conta":
      // TODO(tela-conta): levar para a tela da conta quando ela existir.
      return (
        <AbrirContaAviso
          icone={WalletCards}
          titulo="Você já tem uma conta"
          descricao="Sua conta já foi aberta neste navegador."
          acao={{ rotulo: "Voltar para o início", href: ROUTES.home }}
        />
      );

    case "sucesso":
      return estado.contaAberta && <AbrirContaSucesso conta={estado.contaAberta} />;

    case "formulario":
      return (
        <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-[2.1rem]">Escolha sua conta</h1>
          <p className="mt-3 mb-10 text-muted-foreground">
            Sem tarifa de abertura e sem mensalidade. Você começa com saldo zero.
          </p>
          <AbrirContaForm {...estado} />
        </div>
      );
  }
}
