"use client";

import { useEffect, type ReactNode } from "react";
import { querMovimentoReduzido, ScrollTrigger } from "@/lib/motion/gsap";
import { iniciarRolagemSuave } from "@/lib/motion/rolagem-suave";

/**
 * Liga a rolagem suave **depois** das seções: no React, o efeito do pai roda depois dos efeitos dos filhos,
 * então todos os ScrollTriggers com `pin` já existem quando o Lenis começa (senão a página trava no meio).
 */
export function LandingMovimento({ children }: { children: ReactNode }) {
  useEffect(() => {
    const parar = querMovimentoReduzido() ? undefined : iniciarRolagemSuave();
    ScrollTrigger.refresh();
    return () => parar?.();
  }, []);

  return children;
}
