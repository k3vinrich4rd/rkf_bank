"use client";

import { RotateCw, UserRound, WalletCards } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { ROUTES } from "@/lib/routes";
import { ContaAcoes } from "./conta-acoes";
import { ContaAviso } from "./conta-aviso";
import { ContaSaldo } from "./conta-saldo";
import { ContaShell } from "./conta-shell";
import { useConta, type ContaState } from "./use-conta";

/** Área da conta: saldo, dados e atalhos para as movimentações. O estado vive no `useConta`. */
export function ContaScreen() {
  const estado = useConta();

  return (
    <ContaShell>
      <Conteudo {...estado} />
    </ContaShell>
  );
}

function Conteudo({ etapa, conta, erro, tentarDeNovo }: ContaState) {
  switch (etapa) {
    case "carregando":
      return <ContaCarregando />;

    case "sem-cadastro":
      return (
        <ContaAviso
          icone={UserRound}
          titulo="Primeiro, seu cadastro"
          descricao="Não encontramos sua conta neste navegador. Faça seu cadastro para começar."
          acao={{ rotulo: "Fazer meu cadastro", href: ROUTES.signUp }}
        />
      );

    case "sem-conta":
      return (
        <ContaAviso
          icone={WalletCards}
          titulo="Falta abrir sua conta"
          descricao="Seu cadastro está pronto. Escolha o tipo de conta para começar a usar."
          acao={{ rotulo: "Abrir minha conta", href: ROUTES.abrirConta }}
        />
      );

    case "erro":
      return (
        <div className="flex flex-col gap-4">
          <FormAlert>{erro}</FormAlert>
          <Button type="button" variant="outline" size="xl" onClick={tentarDeNovo}>
            <RotateCw data-icon="inline-start" aria-hidden="true" />
            Tentar de novo
          </Button>
        </div>
      );

    case "pronta":
      return (
        conta && (
          <div className="flex flex-col gap-6 animate-in fade-in slide-in-from-bottom-2 duration-500">
            <h1 className="text-3xl font-semibold tracking-tight">Sua conta</h1>
            <ContaSaldo conta={conta} />
            <ContaAcoes />
          </div>
        )
      );
  }
}

/** Esqueleto no formato da tela pronta, para o conteúdo não "pular" quando a conta chega. */
function ContaCarregando() {
  return (
    <div role="status" aria-label="Carregando sua conta" className="flex flex-col gap-6">
      <div className="h-9 w-40 animate-pulse rounded-lg bg-white/[0.06]" />
      <div className="h-64 animate-pulse rounded-3xl bg-white/[0.04]" />
      <div className="grid grid-cols-4 gap-3">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="h-20 animate-pulse rounded-2xl bg-white/[0.04]" />
        ))}
      </div>
    </div>
  );
}
