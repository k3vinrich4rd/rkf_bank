/**
 * Faz uma palavra ocupar exatamente a largura do pai: mede o texto a 100 px e escala na proporção.
 * `alturaMaxima` (px) limita pela altura da tela, para a palavra não estourar em telas largas e baixas.
 */
export function ajustarPalavra(elemento: HTMLElement, alturaMaxima?: number) {
  const pai = elemento.parentElement;
  if (!pai) return;
  elemento.style.fontSize = "100px";
  const largura = elemento.getBoundingClientRect().width;
  if (largura === 0) return;
  const estiloPai = getComputedStyle(pai);
  const disponivel = pai.clientWidth - parseFloat(estiloPai.paddingLeft) - parseFloat(estiloPai.paddingRight);
  let tamanho = (100 * disponivel) / largura;
  if (alturaMaxima) tamanho = Math.min(tamanho, alturaMaxima / 0.78);
  elemento.style.fontSize = `${tamanho}px`;
}
