import type { ReactNode } from "react";
import { LoaderCircle, RotateCw, WalletCards } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import type { ContaResponse } from "@/lib/api";
import { ROUTES } from "@/lib/routes";
import { ContaAviso } from "./conta-aviso";
import type { ContaState } from "./use-conta";

interface ContaEstadoProps {
  estado: ContaState;
  /** O que mostrar enquanto a conta carrega. Por padrão, um spinner. */
  esqueleto?: ReactNode;
  /** A tela em si, desenhada só quando a conta está pronta. */
  children: (conta: ContaResponse) => ReactNode;
}

/**
 * Etapas comuns a todas as telas da conta (ver `useConta`): carregando, sem conta e erro.
 * Cada tela só se preocupa com o caso feliz, recebido em `children`.
 */
export function ContaEstado({ estado, esqueleto, children }: ContaEstadoProps) {
  const { etapa, conta, erro, tentarDeNovo } = estado;

  switch (etapa) {
    case "carregando":
      return esqueleto ?? (
        <LoaderCircle className="mx-auto size-6 animate-spin text-muted-foreground" aria-label="Carregando" />
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
      return conta && children(conta);
  }
}
