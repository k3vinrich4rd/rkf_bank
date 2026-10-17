"use client";

import { useState } from "react";
import { LoaderCircle, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useLogDeTela } from "@/components/logs";
import { authApi } from "@/lib/api";
import { ROUTES } from "@/lib/routes";

/** Encerra a sessão (apaga os cookies no servidor) e volta para o login. */
export function BotaoSair() {
  const logTela = useLogDeTela("sair");
  const [saindo, setSaindo] = useState(false);

  async function sair() {
    setSaindo(true);
    try {
      await authApi.sair();
    } catch (error) {
      // Mesmo sem resposta, o melhor é ir para o login: o token vence sozinho em poucos minutos.
      logTela.falha("Logout não confirmado pelo servidor", error);
    }
    // Navegação completa (e não `router`), para não sobrar nenhum dado da conta na memória da página.
    window.location.assign(ROUTES.login);
  }

  return (
    <Button type="button" variant="ghost" size="lg" onClick={sair} disabled={saindo} className="text-muted-foreground">
      {saindo ? (
        <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
      ) : (
        <LogOut data-icon="inline-start" aria-hidden="true" />
      )}
      Sair
    </Button>
  );
}
