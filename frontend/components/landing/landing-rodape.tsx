"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { ROUTES } from "@/lib/routes";
import { RODAPE } from "./conteudo";
import { BotaoAbrirConta } from "./landing-nav";
import { useAjustarPalavra } from "./use-ajustar-palavra";

/** Chamada final e o "RKF" gigante, cortado pela borda de baixo, subindo com o scroll. */
export function LandingRodape() {
  const rodape = useRef<HTMLElement>(null);
  const palavra = useRef<HTMLSpanElement>(null);
  useAjustarPalavra(palavra);

  useEffect(() => {
    if (!rodape.current || !palavra.current || querMovimentoReduzido()) return;
    const ctx = gsap.context(() => {
      gsap.fromTo(
        palavra.current,
        { yPercent: 45 },
        { yPercent: 18, ease: "none", scrollTrigger: { trigger: rodape.current, start: "top bottom", end: "bottom bottom", scrub: true } },
      );
    }, rodape);
    return () => ctx.revert();
  }, []);

  return (
    <footer ref={rodape} className="relative overflow-hidden pt-32">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-10">
        <h2 className="text-3xl font-semibold tracking-tight sm:text-5xl">
          {RODAPE.titulo[0]}
          <span className="block text-[var(--l-suave)]">{RODAPE.titulo[1]}</span>
        </h2>
        <div className="mt-10 flex flex-wrap items-center gap-6">
          <BotaoAbrirConta grande />
          <Link href={ROUTES.login} className="text-sm text-[var(--l-suave)] underline-offset-4 hover:text-[var(--l-texto)] hover:underline">
            Já tenho conta · Entrar
          </Link>
        </div>

        <div className="fonte-mono mt-24 flex flex-col gap-2 border-t border-[var(--l-fio)] pt-6 text-xs text-[var(--l-suave)] sm:flex-row sm:justify-between">
          <span>{RODAPE.linha}</span>
          <span>{RODAPE.aviso}</span>
        </div>
      </div>

      <div aria-hidden="true" className="mt-10 px-4 text-center">
        <span ref={palavra} className="palavra fonte-larga text-[var(--l-palavra)]/90">
          RKF
        </span>
      </div>
    </footer>
  );
}
