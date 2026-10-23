"use client";

import { useEffect, useRef } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { CEP } from "./conteudo";

/**
 * Respiro entre as seções presas: o CEP se digita com o scroll, o endereço aparece (como o ViaCEP faz no
 * cadastro de verdade) e uma linha fina percorre os passos até a conta aberta.
 */
export function SecaoCep() {
  const secao = useRef<HTMLElement>(null);

  useEffect(() => {
    if (!secao.current || querMovimentoReduzido()) return;
    const ctx = gsap.context(() => {
      const timeline = gsap.timeline({
        defaults: { ease: "none" },
        scrollTrigger: { trigger: secao.current, start: "top 70%", end: "bottom 80%", scrub: 0.5 },
      });
      timeline
        .fromTo("[data-digito]", { autoAlpha: 0 }, { autoAlpha: 1, stagger: 0.12, duration: 0.05 })
        .fromTo("[data-endereco]", { autoAlpha: 0, y: 12 }, { autoAlpha: 1, y: 0, duration: 0.4, ease: "power2.out" })
        .fromTo("[data-linha]", { scaleX: 0 }, { scaleX: 1, duration: 1.2 }, "<")
        .fromTo("[data-marco]", { autoAlpha: 0.25 }, { autoAlpha: 1, stagger: 0.3, duration: 0.1 }, "<");
    }, secao);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} aria-labelledby="cep-titulo" className="relative py-32 sm:py-48">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-10">
        <p className="fonte-mono text-xs text-[var(--l-suave)]">{CEP.rotulo}</p>
        <h2 id="cep-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
          {CEP.titulo[0]}
          <span className="block text-[var(--l-suave)]">{CEP.titulo[1]}</span>
        </h2>

        <p className="fonte-mono mt-16 text-[clamp(3rem,11vw,9rem)] leading-none font-medium tracking-tight" aria-label={`CEP ${CEP.cep}`}>
          {CEP.cep.split("").map((digito, i) => (
            <span key={i} data-digito aria-hidden="true">
              {digito}
            </span>
          ))}
          <span className="ml-1 inline-block h-[0.8em] w-[0.06em] translate-y-[0.08em] animate-pulse bg-[var(--l-acento)]" aria-hidden="true" />
        </p>
        <p data-endereco className="mt-6 text-lg text-[var(--l-suave)] sm:text-2xl">
          {CEP.endereco}
        </p>

        <div className="relative mt-20">
          <div className="fio-x" />
          <div data-linha className="absolute inset-x-0 top-0 h-px origin-left bg-gradient-to-r from-[var(--l-azul)] to-[var(--l-acento)]" />
          <ol className="fonte-mono mt-4 grid grid-cols-2 gap-y-3 text-xs text-[var(--l-suave)] sm:grid-cols-4">
            {CEP.marcos.map((marco, i) => (
              <li key={marco} data-marco className={i === CEP.marcos.length - 1 ? "text-[var(--l-texto)] sm:text-right" : ""}>
                {String(i + 1).padStart(2, "0")} · {marco}
              </li>
            ))}
          </ol>
        </div>
      </div>
    </section>
  );
}
