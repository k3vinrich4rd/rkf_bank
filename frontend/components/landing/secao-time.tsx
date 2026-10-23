"use client";

import { useEffect, useRef, useState } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { TIME } from "./conteudo";

const RELOGIO = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: "America/Sao_Paulo" });

/** Quem faz o banco. Os nomes sobem por máscara (de baixo para cima) quando a seção entra na tela. */
export function SecaoTime() {
  const secao = useRef<HTMLElement>(null);
  const hora = useHoraDeOsasco();

  useEffect(() => {
    if (!secao.current || querMovimentoReduzido()) return;
    const ctx = gsap.context(() => {
      gsap.fromTo(
        "[data-nome]",
        { yPercent: 105 },
        {
          yPercent: 0,
          duration: 1,
          stagger: 0.12,
          ease: "power3.out",
          scrollTrigger: { trigger: secao.current, start: "top 65%" },
        },
      );
    }, secao);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} aria-labelledby="time-titulo" className="relative py-32 sm:py-40">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-10">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="fonte-mono text-xs text-[var(--l-suave)]">{TIME.rotulo}</p>
            <h2 id="time-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
              {TIME.titulo[0]}
              <span className="block text-[var(--l-suave)]">{TIME.titulo[1]}</span>
            </h2>
          </div>
          <p className="fonte-mono flex items-center gap-2 text-xs text-[var(--l-suave)]">
            <span className="ponto-acento" aria-hidden="true" />
            Osasco, SP · <time suppressHydrationWarning>{hora}</time>
          </p>
        </div>

        <ul className="mt-16">
          {TIME.pessoas.map((pessoa) => (
            <li
              key={pessoa.nome}
              className="grid gap-2 border-t border-[var(--l-fio)] py-8 last:border-b sm:grid-cols-[1fr_auto] sm:items-end"
            >
              <span className="block overflow-hidden">
                <span data-nome className="fonte-larga block text-[clamp(3rem,10vw,8rem)] leading-[0.85] font-extrabold">
                  {pessoa.nome}
                </span>
              </span>
              <span className="fonte-mono text-xs text-[var(--l-suave)] sm:text-right">{pessoa.papel}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

/** Relógio ao vivo de Osasco (mesmo fuso de São Paulo). Vazio no servidor, para não divergir na hidratação. */
function useHoraDeOsasco() {
  const [hora, setHora] = useState("");
  useEffect(() => {
    const atualizar = () => setHora(RELOGIO.format(new Date()));
    atualizar();
    const id = window.setInterval(atualizar, 15_000);
    return () => window.clearInterval(id);
  }, []);
  return hora;
}
