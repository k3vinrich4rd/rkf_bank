"use client";

import { LoaderCircle, RotateCw, WalletCards } from "lucide-react";
import { ContaAviso } from "@/components/conta/conta-aviso";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { ContaShell } from "@/components/conta/conta-shell";
import { ROUTES } from "@/lib/routes";
import { AbrirContaForm } from "./abrir-conta-form";
import { AbrirContaSucesso } from "./abrir-conta-sucesso";
import { useAbrirConta, type AbrirContaState } from "./use-abrir-conta";

/** Abertura de conta: passo seguinte ao cadastro. O estado vive no `useAbrirConta`. */
export function AbrirContaScreen() {
  const estado = useAbrirConta();

  return (
    <ContaShell>
      <Conteudo {...estado} />
    </ContaShell>
  );
}

function Conteudo(estado: AbrirContaState) {
  switch (estado.etapa) {
    case "carregando":
      return (
        <LoaderCircle className="mx-auto size-6 animate-spin text-muted-foreground" aria-label="Carregando" />
      );

    case "erro-conta":
      return (
        <div className="flex flex-col gap-4">
          <FormAlert>{estado.erroConta}</FormAlert>
          <Button type="button" variant="outline" size="xl" onClick={estado.tentarDeNovo}>
            <RotateCw data-icon="inline-start" aria-hidden="true" />
            Tentar de novo
          </Button>
        </div>
      );

    case "ja-tem-conta":
      return (
        <ContaAviso
          icone={WalletCards}
          titulo="Você já tem uma conta"
          descricao="Sua conta já está aberta e pronta para usar."
          acao={{ rotulo: "Ir para minha conta", href: ROUTES.conta }}
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
