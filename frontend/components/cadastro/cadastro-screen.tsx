"use client";

import Link from "next/link";
import { Lock } from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { ROUTES } from "@/lib/routes";
import { CadastroForm } from "./cadastro-form";
import { CadastroShowcase } from "./cadastro-showcase";
import { CadastroSucesso } from "./cadastro-sucesso";
import { useCadastroForm } from "./use-cadastro-form";

/**
 * Tela de cadastro: formulário à esquerda e vitrine à direita (desktop).
 * O estado vive no `useCadastroForm` e é compartilhado entre os dois lados —
 * é assim que o nome digitado aparece no cartão em tempo real.
 */
export function CadastroScreen() {
  const form = useCadastroForm();

  return (
    <div className="grid flex-1 lg:min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,0.85fr)]">
      <section className="flex flex-col px-5 py-6 sm:px-10 lg:px-16 lg:py-8">
        <header className="flex items-center justify-between">
          <Link href={ROUTES.home} aria-label="Voltar para o início" className="rounded-lg">
            <Logo />
          </Link>
          <p className="text-sm text-muted-foreground">
            Já tem conta?{" "}
            <Link href={ROUTES.login} className="font-medium text-foreground underline-offset-4 hover:underline">
              Entrar
            </Link>
          </p>
        </header>

        <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-14">
          {form.cliente ? (
            <CadastroSucesso cliente={form.cliente} logado={form.logado} />
          ) : (
            <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
              <ProgressBar value={form.progresso} />

              <h1 className="mt-8 text-3xl font-semibold tracking-tight sm:text-[2.1rem]">
                Abra sua conta
              </h1>
              <p className="mt-3 mb-10 text-muted-foreground">
                Leva menos de 2 minutos. Sem mensalidade, sem papelada.
              </p>

              <CadastroForm {...form} />
            </div>
          )}
        </main>

        <footer className="flex items-center justify-center gap-2 text-xs text-muted-foreground/70">
          <Lock className="size-3" aria-hidden="true" />
          Seus dados trafegam com criptografia
        </footer>
      </section>

      <CadastroShowcase nome={form.nome} />
    </div>
  );
}

/** Barra fina que avança conforme os campos obrigatórios são preenchidos. */
function ProgressBar({ value }: { value: number }) {
  const percent = Math.round(value * 100);

  return (
    <div className="flex items-center gap-3">
      <div
        role="progressbar"
        aria-label="Progresso do cadastro"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        className="h-1 flex-1 overflow-hidden rounded-full bg-white/[0.06]"
      >
        <div
          className="h-full rounded-full bg-gradient-brand transition-[width] duration-500 ease-out"
          style={{ width: `${percent}%` }}
        />
      </div>
      <span className="w-9 text-right font-mono text-xs text-muted-foreground tabular-nums">{percent}%</span>
    </div>
  );
}
