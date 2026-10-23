import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

/** Registro único do plugin: todo módulo de movimento importa o `gsap` daqui. */
gsap.registerPlugin(ScrollTrigger);

export { gsap, ScrollTrigger };

/** `prefers-reduced-motion`: sem rolagem suave, sem giro, e o conteúdo aparece no estado final. */
export function querMovimentoReduzido(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}
