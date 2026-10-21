"use client";

import { ArrowRight, KeyRound, LoaderCircle } from "lucide-react";
import { ContaShell } from "@/components/conta/conta-shell";
import { ContaVoltar } from "@/components/conta/conta-voltar";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import { useTrocarSenha } from "./use-trocar-senha";

/** Troca da própria senha. O estado vive no `useTrocarSenha`; depois do sucesso a pessoa vai para o login. */
export function TrocarSenhaScreen() {
  const { campo, erroGeral, enviando, handleSubmit } = useTrocarSenha();

  return (
    <ContaShell>
      <div className="flex flex-col animate-in fade-in slide-in-from-bottom-2 duration-500">
        <ContaVoltar />

        <span className="mt-8 grid size-12 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
          <KeyRound className="size-5" aria-hidden="true" />
        </span>
        <h1 className="mt-6 text-3xl font-semibold tracking-tight">Trocar senha</h1>
        <p className="mt-3 mb-8 text-muted-foreground">
          Por segurança, você vai sair de todos os aparelhos e entrar de novo com a senha nova.
        </p>

        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
          <FormField {...campo("senhaAtual")} label="Senha atual" type="password" autoComplete="current-password" autoFocus />
          <FormField
            {...campo("novaSenha")}
            label="Nova senha"
            hint="8 a 72 caracteres"
            type="password"
            autoComplete="new-password"
          />
          <FormField {...campo("confirmarNovaSenha")} label="Repita a nova senha" type="password" autoComplete="new-password" />

          <div className="mt-5 flex flex-col gap-4">
            {erroGeral && <FormAlert>{erroGeral}</FormAlert>}

            <Button type="submit" variant="brand" size="xl" disabled={enviando} className="h-[3.25rem] w-full">
              {enviando ? (
                <>
                  <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
                  Trocando…
                </>
              ) : (
                <>
                  Trocar senha
                  <ArrowRight
                    data-icon="inline-end"
                    aria-hidden="true"
                    className="transition-transform group-hover/button:translate-x-0.5"
                  />
                </>
              )}
            </Button>
          </div>
        </form>
      </div>
    </ContaShell>
  );
}
