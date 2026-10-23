"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

/** Inclinação máxima, em graus. Pouco: o cartão "respira", não gira. */
const INCLINACAO = 12;
/** Fração da distância percorrida por quadro (inércia). */
const SUAVIDADE = 0.08;

/**
 * Envolve o cartão com uma inclinação 3D que segue o cursor (com inércia) e um reflexo de luz que corre por cima.
 * Só CSS 3D (`perspective` + `rotateX/Y`): leve o bastante para telas de formulário. Movimento reduzido → parado.
 */
export function CartaoInterativo({ children, className }: { children: ReactNode; className?: string }) {
  const palco = useRef<HTMLDivElement>(null);
  const cartao = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const elementoPalco = palco.current;
    const elementoCartao = cartao.current;
    if (!elementoPalco || !elementoCartao) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const alvo = { x: 0, y: 0 };
    const atual = { x: 0, y: 0 };
    let quadro = 0;

    const aoMover = (event: PointerEvent) => {
      const caixa = elementoPalco.getBoundingClientRect();
      alvo.x = Math.max(-1, Math.min(1, ((event.clientX - caixa.left) / caixa.width) * 2 - 1));
      alvo.y = Math.max(-1, Math.min(1, ((event.clientY - caixa.top) / caixa.height) * 2 - 1));
    };
    const aoSair = () => {
      alvo.x = 0;
      alvo.y = 0;
    };

    const animar = () => {
      atual.x += (alvo.x - atual.x) * SUAVIDADE;
      atual.y += (alvo.y - atual.y) * SUAVIDADE;
      const estilo = elementoCartao.style;
      estilo.setProperty("--ry", `${atual.x * INCLINACAO}deg`);
      estilo.setProperty("--rx", `${-atual.y * INCLINACAO}deg`);
      estilo.setProperty("--brilho-x", `${50 + atual.x * 45}%`);
      estilo.setProperty("--brilho-y", `${50 + atual.y * 45}%`);
      quadro = requestAnimationFrame(animar);
    };

    // O cursor é ouvido no painel inteiro: o cartão reage antes de o mouse chegar nele.
    const area = elementoPalco.closest("[data-vitrine]") ?? elementoPalco;
    area.addEventListener("pointermove", aoMover as EventListener, { passive: true });
    area.addEventListener("pointerleave", aoSair);
    quadro = requestAnimationFrame(animar);

    return () => {
      cancelAnimationFrame(quadro);
      area.removeEventListener("pointermove", aoMover as EventListener);
      area.removeEventListener("pointerleave", aoSair);
    };
  }, []);

  return (
    <div ref={palco} className={cn("[perspective:1400px]", className)}>
      <div
        ref={cartao}
        className="relative [transform-style:preserve-3d] [transform:rotateX(var(--rx,0deg))_rotateY(var(--ry,0deg))]"
      >
        {children}
        {/* Reflexo: um brilho suave que acompanha a inclinação, como luz batendo no verniz. */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 rounded-2xl mix-blend-overlay [background:radial-gradient(circle_at_var(--brilho-x,30%)_var(--brilho-y,20%),rgb(255_255_255/0.32),transparent_55%)]"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 rounded-2xl [background:linear-gradient(115deg,transparent_35%,rgb(255_255_255/0.07)_45%,transparent_55%)]"
        />
      </div>
    </div>
  );
}
