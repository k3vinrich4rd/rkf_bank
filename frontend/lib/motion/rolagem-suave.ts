import Lenis from "lenis";
import { gsap, ScrollTrigger } from "./gsap";

/**
 * Rolagem suave (Lenis) ligada ao relógio do GSAP, para scroll e animações andarem no mesmo quadro.
 *
 * ⚠️ Ordem importa: chame **depois** de criar os ScrollTriggers com `pin`. Se o Lenis medir a página antes de os
 * pins acrescentarem a altura deles, a rolagem trava no meio. Por isso também refazemos as medidas no `refresh`.
 */
export function iniciarRolagemSuave(): () => void {
  const lenis = new Lenis({ lerp: 0.1, wheelMultiplier: 0.9 });
  const aoRolar = () => ScrollTrigger.update();
  const aoAtualizar = () => lenis.resize();
  const quadro = (tempo: number) => lenis.raf(tempo * 1000);

  lenis.on("scroll", aoRolar);
  ScrollTrigger.addEventListener("refresh", aoAtualizar);
  gsap.ticker.add(quadro);
  gsap.ticker.lagSmoothing(0);

  return () => {
    gsap.ticker.remove(quadro);
    ScrollTrigger.removeEventListener("refresh", aoAtualizar);
    lenis.destroy();
  };
}
