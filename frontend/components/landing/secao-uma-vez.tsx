"use client";

import { useEffect, useRef } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { UMA_VEZ } from "./conteudo";

/**
 * Idempotência contada como produto: o mesmo pedido chega três vezes (rede caiu, clique duplo) e o débito
 * continua um só. O contador de envios sobe com o scroll; o número gigante não sai do 1.
 */
export function SecaoUmaVez() {
  const secao = useRef<HTMLElement>(null);
  const envios = useRef<HTMLSpanElement>(null);
  const fio = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!secao.current || !envios.current || !fio.current) return;
    const elementoEnvios = envios.current;
    const linhas = secao.current.querySelectorAll<HTMLElement>("[data-tentativa]");
    const total = UMA_VEZ.tentativas.length;

    if (querMovimentoReduzido()) {
      elementoEnvios.textContent = String(total);
      return;
    }

    const ctx = gsap.context(() => {
      const contador = { n: 1 };
      const timeline = gsap.timeline({
        defaults: { ease: "power2.out" },
        scrollTrigger: { trigger: secao.current, start: "top top", end: "+=130%", pin: true, scrub: 0.6 },
      });

      linhas.forEach((linha, i) => {
        timeline.fromTo(linha, { autoAlpha: 0, x: -16 }, { autoAlpha: 1, x: 0, duration: 0.8 }, i);
      });
      timeline
        .fromTo(fio.current, { scaleY: 0 }, { scaleY: 1, duration: total - 1 }, 0.6)
        .to(
          contador,
          {
            n: total,
            duration: total - 1,
            ease: "none",
            onUpdate: () => {
              elementoEnvios.textContent = String(Math.round(contador.n));
            },
          },
          0.6,
        )
        .to({}, { duration: 0.8 });
    }, secao);

    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} id="seguranca" aria-labelledby="seguranca-titulo" className="relative flex min-h-svh items-center py-24">
      <div className="mx-auto grid w-full max-w-7xl gap-14 px-4 sm:px-10 lg:grid-cols-2 lg:items-center lg:gap-20">
        <div>
          <p className="fonte-mono text-xs text-[var(--l-suave)]">{UMA_VEZ.rotulo}</p>
          <h2 id="seguranca-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
            {UMA_VEZ.titulo[0]}
            <span className="block text-[var(--l-suave)]">{UMA_VEZ.titulo[1]}</span>
          </h2>

          <div className="mt-12 flex items-end gap-10">
            <div>
              <span ref={envios} className="fonte-mono block text-6xl leading-none text-[var(--l-suave)] sm:text-7xl">
                1
              </span>
              <span className="fonte-mono mt-3 block text-xs text-[var(--l-suave)]">envios</span>
            </div>
            <div>
              <span className="fonte-larga block text-[clamp(7rem,18vw,13rem)] leading-[0.78] font-extrabold">1</span>
              <span className="fonte-mono mt-3 flex items-center gap-2 text-xs">
                <span className="ponto-acento" aria-hidden="true" />
                débito
              </span>
            </div>
          </div>
        </div>

        <div>
          <div className="relative">
            <div
              ref={fio}
              aria-hidden="true"
              className="absolute top-7 bottom-7 left-[0.2rem] w-px origin-top bg-[var(--l-acento)]/60"
            />
          <ol className="fonte-mono text-sm">
            {UMA_VEZ.tentativas.map((tentativa, i) => (
              <li
                key={tentativa.hora}
                data-tentativa
                className="relative grid grid-cols-[1.2rem_5rem_1fr] items-baseline gap-3 border-t border-[var(--l-fio)] py-4 last:border-b sm:grid-cols-[1.2rem_6rem_1fr_auto]"
              >
                <span
                  aria-hidden="true"
                  className={`size-[0.45rem] rounded-full ${i === 0 ? "ponto-acento" : "border border-[var(--l-acento)]/70"}`}
                />
                <span className="text-[var(--l-suave)]">{tentativa.hora}</span>
                <span>{tentativa.acao}</span>
                <span
                  className={`col-span-2 col-start-2 sm:col-span-1 sm:col-start-auto ${
                    i === 0 ? "text-[var(--l-texto)]" : "text-[var(--l-suave)]"
                  }`}
                >
                  {tentativa.resultado}
                </span>
              </li>
            ))}
          </ol>
          </div>
          <p className="mt-6 max-w-md text-sm text-[var(--l-suave)]">{UMA_VEZ.nota}</p>
          <ul className="fonte-mono mt-10 grid gap-x-6 text-xs text-[var(--l-suave)] sm:grid-cols-3">
            {UMA_VEZ.specs.map((spec) => (
              <li key={spec} className="border-t border-[var(--l-fio)] py-2">
                {spec}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
