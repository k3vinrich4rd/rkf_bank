"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ROUTES } from "@/lib/routes";
import { NAV } from "./conteudo";

type Modo = "topo" | "escondida" | "visivel";

/** Folga em px antes de esconder: evita a nav piscar com rolagens mínimas. */
const FOLGA = 8;

/**
 * Fixa no topo e transparente no início. Ao descer, sobe para fora da tela; ao subir, volta com fundo e
 * desfoque. Assim o conteúdo nunca passa por baixo dos links.
 */
export function LandingNav() {
  const [modo, setModo] = useState<Modo>("topo");

  useEffect(() => {
    let ultimo = window.scrollY;
    const aoRolar = () => {
      const atual = window.scrollY;
      if (atual < 40) setModo("topo");
      else if (atual > ultimo + FOLGA) setModo("escondida");
      else if (atual < ultimo - FOLGA) setModo("visivel");
      if (Math.abs(atual - ultimo) > FOLGA) ultimo = atual;
    };
    window.addEventListener("scroll", aoRolar, { passive: true });
    return () => window.removeEventListener("scroll", aoRolar);
  }, []);

  const estilo = {
    topo: "border-transparent bg-transparent",
    escondida: "-translate-y-full border-transparent",
    visivel: "border-[var(--l-fio)] bg-[#0a0f1c]/75 backdrop-blur-xl",
  }[modo];

  return (
    <header className={`fixed inset-x-0 top-0 z-50 border-b transition-[translate,background-color,border-color] duration-500 ${estilo}`}>
      <nav aria-label="Principal" className="flex h-16 items-center justify-between px-4 sm:px-10">
        <Link href={ROUTES.home} className="flex items-baseline gap-2 rounded-lg">
          <span className="text-lg font-bold tracking-[0.2em]">RKF</span>
          <span className="hidden text-xs text-[var(--l-suave)] sm:inline">Banco Digital</span>
        </Link>

        <ul className="hidden items-center gap-8 text-sm text-[var(--l-suave)] md:flex">
          {NAV.map(({ rotulo, href }) => (
            <li key={href}>
              <a href={href} className="transition-colors hover:text-[var(--l-texto)]">
                {rotulo}
              </a>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-2 sm:gap-4">
          <Link href={ROUTES.login} className="px-2 py-2 text-sm text-[var(--l-suave)] transition-colors hover:text-[var(--l-texto)]">
            Entrar
          </Link>
          <BotaoAbrirConta />
        </div>
      </nav>
    </header>
  );
}

/** Botão claro com o ponto de acento: o mesmo na nav e no rodapé. */
export function BotaoAbrirConta({ grande = false }: { grande?: boolean }) {
  return (
    <Link
      href={ROUTES.signUp}
      className={`inline-flex items-center gap-2.5 rounded-full bg-[var(--l-texto)] font-medium text-[#0a0f1c] transition-transform hover:scale-[1.03] active:scale-[0.98] ${
        grande ? "px-7 py-4 text-base" : "px-4 py-2 text-sm"
      }`}
    >
      <span className="ponto-acento" aria-hidden="true" />
      Abrir conta
    </Link>
  );
}
