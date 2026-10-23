"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { ArrowRight, CircleCheck, Clock, LoaderCircle, type LucideIcon } from "lucide-react";
import { AuthShell } from "@/components/auth/auth-shell";
import { CampoFlutuante } from "@/components/auth/campo-flutuante";
import { VitrineCartao } from "@/components/auth/vitrine-cartao";
import { FormAlert } from "@/components/ui/form-alert";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";
import { useLogin } from "./use-login";

interface LoginScreenProps {
  /** Para onde ir depois de entrar, já validado (só caminhos internos). */
  destino: string;
  /** Chegou aqui porque o token venceu no meio do uso. */
  expirou: boolean;
  /** Chegou aqui logo depois de trocar a senha (a sessão antiga cai junto). */
  senhaTrocada: boolean;
}

/** Entrada em sequência: cada bloco aparece um pouco depois do anterior. */
const ENTRADA = "animate-in fade-in slide-in-from-bottom-3 fill-mode-both duration-700";

/** Login com CPF e senha. O estado vive no `useLogin`; aqui é só o desenho. */
export function LoginScreen({ destino, expirou, senhaTrocada }: LoginScreenProps) {
  const { campo, erroGeral, enviando, handleSubmit } = useLogin(destino);

  return (
    <AuthShell
      atalho={{ pergunta: "Ainda não tem conta?", rotulo: "Abrir conta", href: ROUTES.signUp }}
      vitrine={
        <VitrineCartao
          titular="Cliente RKF"
          rodape="Sessão de 15 minutos  ·  senha guardada só como hash  ·  limite de tentativas"
        />
      }
    >
      <div className="w-full max-w-[22.5rem] self-center lg:self-start">
        <h1 className={`text-[2.75rem] leading-[1.05] font-semibold tracking-[-0.035em] text-balance ${ENTRADA}`}>
          Bem-vindo
          <span className="block text-muted-foreground">de volta.</span>
        </h1>
        <p className={`mt-5 text-[0.95rem] text-muted-foreground delay-75 ${ENTRADA}`}>
          Entre com o CPF e a senha do seu cadastro.
        </p>

        {senhaTrocada && (
          <Aviso icone={CircleCheck} tom="sucesso">
            Senha trocada. Entre com a senha nova.
          </Aviso>
        )}
        {expirou && (
          <Aviso icone={Clock} tom="info">
            Sua sessão expirou. Entre de novo para continuar de onde parou.
          </Aviso>
        )}

        <form onSubmit={handleSubmit} noValidate className={`mt-10 flex flex-col gap-3 delay-150 ${ENTRADA}`}>
          <CampoFlutuante {...campo("cpf")} label="CPF" inputMode="numeric" autoComplete="username" autoFocus />
          <CampoFlutuante {...campo("senha")} label="Senha" senha autoComplete="current-password" />

          {erroGeral && (
            <div className="mt-2">
              <FormAlert>{erroGeral}</FormAlert>
            </div>
          )}

          <button
            type="submit"
            disabled={enviando}
            className={cn(
              "group mt-4 inline-flex h-[3.75rem] w-full items-center justify-center gap-2.5 rounded-2xl",
              "bg-foreground text-[0.95rem] font-medium text-background",
              "shadow-[0_10px_40px_-10px_rgb(241_245_249/0.25)] hover:shadow-[0_14px_50px_-10px_rgb(241_245_249/0.35)]",
              "transition-[transform,box-shadow,opacity] duration-200 active:scale-[0.99]",
              "disabled:cursor-wait disabled:opacity-70",
            )}
          >
            {enviando ? (
              <>
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                Entrando…
              </>
            ) : (
              <>
                Entrar
                <ArrowRight className="size-4 transition-transform duration-200 group-hover:translate-x-0.5" aria-hidden="true" />
              </>
            )}
          </button>
        </form>

        <p className={`mt-8 text-sm text-muted-foreground delay-200 ${ENTRADA}`}>
          Primeira vez no RKF?{" "}
          <Link href={ROUTES.signUp} className="font-medium text-foreground underline-offset-4 hover:underline">
            Abra sua conta em minutos
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}

const TONS = {
  sucesso: "border-emerald-500/20 bg-emerald-500/[0.06] [&>svg]:text-emerald-400",
  info: "border-brand-blue/20 bg-brand-blue/[0.06] [&>svg]:text-brand-cyan",
} as const;

function Aviso({ icone: Icone, tom, children }: { icone: LucideIcon; tom: keyof typeof TONS; children: ReactNode }) {
  return (
    <p
      role="status"
      className={`mt-8 flex items-start gap-2.5 rounded-2xl border px-4 py-3.5 text-sm text-foreground/90 animate-in fade-in slide-in-from-top-1 duration-300 ${TONS[tom]}`}
    >
      <Icone className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {children}
    </p>
  );
}
