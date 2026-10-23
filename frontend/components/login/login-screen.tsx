"use client";

import type { ReactNode } from "react";
import { ArrowRight, CircleCheck, Clock, LoaderCircle, type LucideIcon } from "lucide-react";
import { AuthShell } from "@/components/auth/auth-shell";
import { CartaoEmVitrine } from "@/components/auth/cartao-em-vitrine";
import { Vitrine } from "@/components/auth/vitrine";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import { PasswordField } from "@/components/ui/password-field";
import { ROUTES } from "@/lib/routes";
import { useLogin } from "./use-login";

interface LoginScreenProps {
  /** Para onde ir depois de entrar, já validado (só caminhos internos). */
  destino: string;
  /** Chegou aqui porque o token venceu no meio do uso. */
  expirou: boolean;
  /** Chegou aqui logo depois de trocar a senha (a sessão antiga cai junto). */
  senhaTrocada: boolean;
}

/** Fatos de segurança do login: todos verdadeiros no backend (contrato 2.0 a 2.3). */
const FATOS = [
  "Sessão que expira sozinha em 15 minutos",
  "Senha guardada só como hash, nunca em texto",
  "5 senhas erradas seguidas · pausa de 15 minutos",
] as const;

/** Login com CPF e senha. O estado vive no `useLogin`. */
export function LoginScreen({ destino, expirou, senhaTrocada }: LoginScreenProps) {
  const { campo, erroGeral, enviando, handleSubmit } = useLogin(destino);

  return (
    <AuthShell
      atalho={{ pergunta: "Ainda não tem conta?", rotulo: "Abrir conta", href: ROUTES.signUp }}
      vitrine={
        <Vitrine
          rotulo="Acesso seguro"
          destaque={<CartaoEmVitrine titular="Cliente RKF" legenda="Seu saldo, seu extrato e suas transferências, a um login." />}
          titulo={
            <>
              Seu dinheiro continua <span className="text-gradient-brand">onde você deixou.</span>
            </>
          }
          fatos={FATOS}
        />
      }
    >
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
        <p className="font-mono text-xs tracking-[0.2em] text-brand-cyan uppercase">Entrar</p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight text-balance sm:text-[2.6rem] sm:leading-[1.1]">
          Bem-vindo de volta.
        </h1>
        <p className="mt-3 text-muted-foreground">Use o CPF e a senha do seu cadastro.</p>

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

        <form onSubmit={handleSubmit} noValidate className="mt-10 flex flex-col gap-5">
          <FormField
            {...campo("cpf")}
            label="CPF"
            placeholder="000.000.000-00"
            inputMode="numeric"
            autoComplete="username"
            autoFocus
          />
          <PasswordField {...campo("senha")} label="Senha" autoComplete="current-password" />

          <div className="mt-4 flex flex-col gap-4">
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

        {/* No celular a vitrine some: os três fatos viram uma linha discreta embaixo do formulário. */}
        <ul className="mt-10 grid grid-cols-3 gap-3 border-t border-white/[0.06] pt-6 text-center font-mono text-[0.68rem] text-muted-foreground lg:hidden">
          <li>Sessão de 15 min</li>
          <li>Senha com hash</li>
          <li>Limite de tentativas</li>
        </ul>
      </div>
    </AuthShell>
  );
}

const TONS = {
  sucesso: "border-emerald-500/25 bg-emerald-500/[0.07] [&>svg]:text-emerald-400",
  info: "border-brand-blue/25 bg-brand-blue/[0.07] [&>svg]:text-brand-cyan",
} as const;

function Aviso({ icone: Icone, tom, children }: { icone: LucideIcon; tom: keyof typeof TONS; children: ReactNode }) {
  return (
    <p
      role="status"
      className={`mt-8 flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm text-foreground/90 animate-in fade-in slide-in-from-top-1 duration-300 ${TONS[tom]}`}
    >
      <Icone className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {children}
    </p>
  );
}
