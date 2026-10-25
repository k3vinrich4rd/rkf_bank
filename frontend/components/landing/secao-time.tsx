"use client";

import { useEffect, useRef, useState } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import { TIME, type Pessoa } from "./conteudo";

const RELOGIO = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: "America/Sao_Paulo" });

/**
 * Sobre: a história dos três amigos e quem faz o quê. Os nomes sobem por máscara (de baixo para cima) quando a
 * seção entra na tela; os desenvolvedores levam o link do GitHub.
 */
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
          scrollTrigger: { trigger: "[data-pessoas]", start: "top 75%" },
        },
      );
      gsap.fromTo(
        "[data-historia]",
        { autoAlpha: 0, y: 16 },
        {
          autoAlpha: 1,
          y: 0,
          duration: 0.8,
          stagger: 0.12,
          ease: "power2.out",
          scrollTrigger: { trigger: secao.current, start: "top 65%" },
        },
      );
    }, secao);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} id="sobre" aria-labelledby="sobre-titulo" className="relative scroll-mt-16 py-32 sm:py-40">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-10">
        <div className="grid gap-10 lg:grid-cols-[1fr_1fr] lg:items-end">
          <div>
            <p className="fonte-mono text-xs text-[var(--l-suave)]">{TIME.rotulo}</p>
            <h2 id="sobre-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
              {TIME.titulo[0]}
              <span className="block text-[var(--l-suave)]">{TIME.titulo[1]}</span>
            </h2>
          </div>
          <div className="flex flex-col gap-4 text-[var(--l-suave)] sm:text-lg">
            {TIME.historia.map((paragrafo) => (
              <p key={paragrafo} data-historia className="text-pretty">
                {paragrafo}
              </p>
            ))}
            <p data-historia className="fonte-mono flex items-center gap-2 text-xs">
              <span className="ponto-acento" aria-hidden="true" />
              Osasco, SP · <time suppressHydrationWarning>{hora}</time>
            </p>
          </div>
        </div>

        <ul data-pessoas className="mt-20">
          {TIME.pessoas.map((pessoa) => (
            <LinhaPessoa key={pessoa.nome} pessoa={pessoa} />
          ))}
        </ul>
      </div>
    </section>
  );
}

function LinhaPessoa({ pessoa }: { pessoa: Pessoa }) {
  return (
    <li className="grid gap-4 border-t border-[var(--l-fio)] py-8 last:border-b sm:grid-cols-[1fr_auto] sm:items-end sm:gap-8">
      <div>
        <p className="fonte-mono text-xs text-[var(--l-suave)]">{pessoa.papel}</p>
        <span className="mt-3 block overflow-hidden">
          <span data-nome className="fonte-larga block text-[clamp(2.2rem,4.3vw,4.25rem)] leading-[0.95] font-extrabold sm:whitespace-nowrap">
            {pessoa.nome}
          </span>
        </span>
      </div>
      <div className="flex flex-col gap-2 sm:items-end sm:text-right">
        <span className="fonte-mono text-xs text-[var(--l-suave)]">{pessoa.faz}</span>
        {pessoa.github && (
          <a
            href={pessoa.github.url}
            target="_blank"
            rel="noopener noreferrer"
            className="group fonte-mono inline-flex items-center gap-2 self-start text-sm transition-colors hover:text-[var(--l-acento)] sm:self-end"
          >
            <IconeGithub />
            github.com/{pessoa.github.usuario}
            <span aria-hidden="true" className="transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5">
              ↗
            </span>
            <span className="sr-only">(abre em nova aba)</span>
          </a>
        )}
      </div>
    </li>
  );
}

/** Marca do GitHub em SVG (o lucide-react não traz ícones de marca). */
function IconeGithub() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="size-4 fill-current">
      <path d="M12 .5C5.65.5.5 5.65.5 12a11.5 11.5 0 0 0 7.86 10.92c.58.1.79-.25.79-.56v-2c-3.2.7-3.87-1.37-3.87-1.37-.53-1.33-1.28-1.69-1.28-1.69-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.71 1.26 3.37.96.1-.75.4-1.26.73-1.55-2.55-.29-5.24-1.28-5.24-5.68 0-1.25.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.04 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.77 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.58.23 2.75.11 3.04.74.81 1.19 1.84 1.19 3.09 0 4.41-2.69 5.39-5.26 5.67.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5Z" />
    </svg>
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
