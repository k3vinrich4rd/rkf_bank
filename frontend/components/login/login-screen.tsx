"use client";

import Link from "next/link";
import { ArrowRight, Clock, LoaderCircle, LockKeyhole } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import { ROUTES } from "@/lib/routes";
import { useLogin } from "./use-login";

interface LoginScreenProps {
  /** Para onde ir depois de entrar, já validado (só caminhos internos). */
  destino: string;
  /** Chegou aqui porque o token venceu no meio do uso. */
  expirou: boolean;
}

/** Login com CPF e senha. O estado vive no `useLogin`. */
export function LoginScreen({ destino, expirou }: LoginScreenProps) {
  const { campo, erroGeral, enviando, handleSubmit } = useLogin(destino);

  return (
    <main className="relative isolate grid flex-1 place-items-center px-4 py-16">
      <div
        aria-hidden="true"
        className="absolute top-1/4 left-1/2 -z-10 size-[30rem] -translate-x-1/2 rounded-full bg-brand-blue/15 blur-[120px]"
      />

      <div className="w-full max-w-sm rounded-3xl border border-border bg-surface/80 p-8 backdrop-blur animate-in fade-in slide-in-from-bottom-2 duration-500">
        <Link href={ROUTES.home} className="inline-flex rounded-lg" aria-label="Voltar para o início">
          <Logo />
        </Link>

        <span className="mt-8 grid size-12 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
          <LockKeyhole className="size-5" aria-hidden="true" />
        </span>

        <h1 className="mt-6 text-2xl font-semibold tracking-tight">Entrar na sua conta</h1>
        <p className="mt-2 text-sm text-muted-foreground">Use o CPF e a senha do seu cadastro.</p>

        {expirou && (
          <p
            role="status"
            className="mt-6 flex items-start gap-2.5 rounded-xl border border-brand-blue/25 bg-brand-blue/[0.07] px-4 py-3 text-sm text-foreground/90"
          >
            <Clock className="mt-0.5 size-4 shrink-0 text-brand-cyan" aria-hidden="true" />
            Sua sessão expirou. Entre de novo para continuar.
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate className="mt-8 flex flex-col gap-5">
          <FormField
            {...campo("cpf")}
            label="CPF"
            placeholder="000.000.000-00"
            inputMode="numeric"
            autoComplete="username"
            autoFocus
          />
          <FormField {...campo("senha")} label="Senha" type="password" autoComplete="current-password" />

          <div className="mt-3 flex flex-col gap-4">
            {erroGeral && <FormAlert>{erroGeral}</FormAlert>}

            <Button type="submit" variant="brand" size="xl" disabled={enviando} className="h-[3.25rem] w-full">
              {enviando ? (
                <>
                  <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
                  Entrando…
                </>
              ) : (
                <>
                  Entrar
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

        <p className="mt-8 text-center text-sm text-muted-foreground">
          Ainda não tem conta?{" "}
          <Link href={ROUTES.signUp} className="font-medium text-foreground underline-offset-4 hover:underline">
            Abrir minha conta
          </Link>
        </p>
      </div>
    </main>
  );
}
