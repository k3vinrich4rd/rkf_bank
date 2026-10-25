import { useEffect, type RefObject } from "react";
import { ajustarPalavra } from "@/lib/motion/ajustar-palavra";
import { ScrollTrigger } from "@/lib/motion/gsap";

/**
 * Mantém a palavra gigante na largura do pai: depois das fontes carregarem e a cada mudança de tamanho.
 * Recalcular muda a altura do layout, então pede um `refresh` aos ScrollTriggers.
 */
export function useAjustarPalavra(ref: RefObject<HTMLElement | null>, alturaMaxima?: () => number) {
  useEffect(() => {
    const elemento = ref.current;
    const pai = elemento?.parentElement;
    if (!elemento || !pai) return;

    let ultimaLargura = -1;
    const ajustar = () => {
      if (pai.clientWidth === ultimaLargura) return;
      ultimaLargura = pai.clientWidth;
      ajustarPalavra(elemento, alturaMaxima?.());
      ScrollTrigger.refresh();
    };

    const observador = new ResizeObserver(ajustar);
    observador.observe(pai);
    document.fonts.ready.then(() => {
      ultimaLargura = -1;
      ajustar();
    });
    return () => observador.disconnect();
  }, [ref, alturaMaxima]);
}
