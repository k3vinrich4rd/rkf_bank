"use client";

import { AuthShell } from "@/components/auth/auth-shell";
import { BankCard } from "@/components/marketing/bank-card";
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
    <AuthShell
      atalho={{ pergunta: "Já tem conta?", rotulo: "Entrar", href: ROUTES.login }}
      vitrine={<CadastroShowcase nome={form.nome} />}
    >
      {form.cliente ? (
        <CadastroSucesso cliente={form.cliente} logado={form.logado} />
      ) : (
        <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
          {/* No celular a vitrine some: o cartão com o nome vem para cima do formulário, menor. */}
          <BankCard
            holder={form.nome || "Seu nome aqui"}
            className="mx-auto mb-10 max-w-[17rem] -rotate-3 shadow-[0_30px_60px_-20px_rgb(0_0_0/0.8)] lg:hidden"
          />

          <ProgressBar value={form.progresso} />

          <p className="mt-8 font-mono text-xs tracking-[0.2em] text-brand-cyan uppercase">Abrir conta</p>
          <h1 className="mt-4 text-4xl font-semibold tracking-tight text-balance sm:text-[2.6rem] sm:leading-[1.1]">
            Sua conta em poucos minutos.
          </h1>
          <p className="mt-3 mb-10 text-muted-foreground">Sem mensalidade e sem papelada. Três blocos e pronto.</p>

          <CadastroForm {...form} />
        </div>
      )}
    </AuthShell>
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
          className="h-full rounded-full bg-gradient-brand shadow-[0_0_12px_rgb(6_182_212/0.6)] transition-[width] duration-500 ease-out"
          style={{ width: `${percent}%` }}
        />
      </div>
      <span className="w-9 text-right font-mono text-xs text-muted-foreground tabular-nums">{percent}%</span>
    </div>
  );
}
