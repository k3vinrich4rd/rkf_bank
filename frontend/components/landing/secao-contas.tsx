"use client";

import { useEffect, useRef } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { CONTAS } from "./conteudo";

/** Três colunas desenhadas só com fios: os fios se desenham e as linhas entram em sequência. */
export function SecaoContas() {
  const secao = useRef<HTMLElement>(null);

  useEffect(() => {
    if (!secao.current || querMovimentoReduzido()) return;
    const ctx = gsap.context(() => {
      const timeline = gsap.timeline({
        defaults: { ease: "power2.out" },
        scrollTrigger: { trigger: "[data-planos]", start: "top 75%" },
      });
      timeline
        .fromTo("[data-fio-h]", { scaleX: 0 }, { scaleX: 1, duration: 0.9, stagger: 0.12 })
        .fromTo("[data-fio-v]", { scaleY: 0 }, { scaleY: 1, duration: 0.9, stagger: 0.12 }, "<0.2")
        .fromTo("[data-item]", { autoAlpha: 0, y: 10 }, { autoAlpha: 1, y: 0, duration: 0.5, stagger: 0.05 }, "<0.3");
    }, secao);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} id="contas" aria-labelledby="contas-titulo" className="relative py-32 sm:py-40">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-10">
        <p className="fonte-mono text-xs text-[var(--l-suave)]">{CONTAS.rotulo}</p>
        <h2 id="contas-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
          {CONTAS.titulo[0]}
          <span className="block text-[var(--l-suave)]">{CONTAS.titulo[1]}</span>
        </h2>

        <div data-planos className="mt-16 grid md:grid-cols-3">
          {CONTAS.planos.map((plano, i) => (
            <article key={plano.nome} className="relative px-0 pt-8 pb-10 md:px-8 md:first:pl-0">
              <span data-fio-h aria-hidden="true" className="absolute inset-x-0 top-0 h-px origin-left bg-[var(--l-fio)]" />
              {i > 0 && (
                <span data-fio-v aria-hidden="true" className="absolute inset-y-0 left-0 hidden w-px origin-top bg-[var(--l-fio)] md:block" />
              )}
              <h3 data-item className="flex items-center gap-2 text-sm text-[var(--l-suave)]">
                {plano.destaque && <span className="ponto-acento" aria-hidden="true" />}
                {plano.nome}
              </h3>
              <p data-item className="mt-6 text-4xl font-semibold tracking-tight sm:text-5xl">
                {plano.preco}
                {plano.sufixo && <span className="ml-1 text-base font-normal text-[var(--l-suave)]">{plano.sufixo}</span>}
              </p>
              <ul className="fonte-mono mt-8 text-xs text-[var(--l-suave)]">
                {plano.itens.map((item) => (
                  <li key={item} data-item className="border-t border-[var(--l-fio)] py-3">
                    {item}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
