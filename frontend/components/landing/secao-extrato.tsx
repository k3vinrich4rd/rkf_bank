"use client";

import { useEffect, useRef } from "react";
import { gsap, querMovimentoReduzido } from "@/lib/motion/gsap";
import {
  EXTRATO_ANTIGO,
  EXTRATO_NOVO,
  SALDO_FINAL,
  SALDO_INICIAL,
  formatarLancamento,
  formatarReais,
  type Lancamento,
} from "./conteudo";

/** Do mais novo para o mais antigo, como num extrato: os de hoje em cima, os de ontem embaixo. */
const LINHAS = [...EXTRATO_NOVO].reverse();

/**
 * O extrato que se escreve: com o scroll, cada lançamento de hoje entra por cima, empurra os antigos e o saldo
 * anda até o valor final (o mesmo do app no hero). O mais novo leva o ponto de acento.
 */
export function SecaoExtrato() {
  const secao = useRef<HTMLElement>(null);
  const saldo = useRef<HTMLParagraphElement>(null);
  const lista = useRef<HTMLUListElement>(null);

  useEffect(() => {
    if (!secao.current || !saldo.current || !lista.current) return;
    const elementoSaldo = saldo.current;
    const novas = Array.from(lista.current.querySelectorAll<HTMLElement>("[data-nova]")).reverse();
    const pontos = novas.map((linha) => linha.querySelector<HTMLElement>("[data-ponto]"));

    if (querMovimentoReduzido()) {
      elementoSaldo.textContent = formatarReais(SALDO_FINAL);
      pontos.at(-1)?.style.setProperty("visibility", "visible");
      return;
    }

    const ctx = gsap.context(() => {
      const contador = { valor: SALDO_INICIAL };
      const escreverSaldo = () => {
        elementoSaldo.textContent = formatarReais(Math.round(contador.valor * 100) / 100);
      };
      escreverSaldo();

      const timeline = gsap.timeline({
        defaults: { ease: "power2.out" },
        scrollTrigger: { trigger: secao.current, start: "top top", end: "+=160%", pin: true, scrub: 0.6 },
      });

      let saldoAcumulado = SALDO_INICIAL;
      novas.forEach((linha, i) => {
        saldoAcumulado += EXTRATO_NOVO[i]?.valor ?? 0;
        const momento = i * 1;
        timeline
          .fromTo(linha, { height: 0, autoAlpha: 0 }, { height: "auto", autoAlpha: 1, duration: 0.8 }, momento)
          .to(contador, { valor: saldoAcumulado, duration: 0.8, onUpdate: escreverSaldo }, momento);
        const ponto = pontos[i];
        const anterior = pontos[i - 1];
        if (ponto) timeline.fromTo(ponto, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.3 }, momento + 0.4);
        if (anterior) timeline.to(anterior, { autoAlpha: 0, duration: 0.3 }, momento + 0.4);
      });
      timeline.to({}, { duration: 0.8 });
    }, secao);

    return () => ctx.revert();
  }, []);

  return (
    <section ref={secao} id="extrato" aria-labelledby="extrato-titulo" className="relative flex min-h-svh items-center py-24">
      <div className="mx-auto grid w-full max-w-7xl gap-12 px-4 sm:px-10 lg:grid-cols-[1.1fr_1fr] lg:items-center lg:gap-20">
        <div>
          <p className="fonte-mono text-xs text-[var(--l-suave)]">02 — Extrato</p>
          <h2 id="extrato-titulo" className="mt-4 text-3xl font-semibold tracking-tight sm:text-5xl">
            Depósito, saque e transferência.
            <span className="block text-[var(--l-suave)]">Num extrato só.</span>
          </h2>
          <p className="mt-10 text-xs text-[var(--l-suave)]">Saldo disponível</p>
          <p
            ref={saldo}
            aria-live="off"
            className="fonte-mono mt-2 text-[clamp(2.4rem,7vw,6rem)] leading-none font-medium tracking-tight"
          >
            {formatarReais(SALDO_FINAL)}
          </p>
        </div>

        <div className="relative h-[24rem] overflow-hidden [mask-image:linear-gradient(to_bottom,black_70%,transparent)] sm:h-[28rem]">
          <ul ref={lista} className="fonte-mono text-sm">
            {LINHAS.map((lancamento) => (
              <LinhaExtrato key={lancamento.hora} lancamento={lancamento} nova />
            ))}
            {EXTRATO_ANTIGO.map((lancamento) => (
              <LinhaExtrato key={lancamento.hora} lancamento={lancamento} />
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

function LinhaExtrato({ lancamento, nova = false }: { lancamento: Lancamento; nova?: boolean }) {
  const entrada = lancamento.valor > 0;
  return (
    <li data-nova={nova ? "" : undefined} className="overflow-hidden">
      <div className="grid grid-cols-[5.5rem_1fr_auto] items-center gap-4 border-t border-[var(--l-fio)] py-4 sm:grid-cols-[7rem_1fr_auto]">
        <span className="text-[var(--l-suave)]">{lancamento.hora}</span>
        <span className="flex items-center gap-2 truncate">
          {/* O ponto existe em toda linha (invisível nas antigas) para as descrições ficarem alinhadas. */}
          <span data-ponto={nova ? "" : undefined} className="ponto-acento invisible shrink-0" aria-hidden="true" />
          {lancamento.descricao}
        </span>
        <span className={entrada ? "text-emerald-400" : "text-[var(--l-texto)]"}>{formatarLancamento(lancamento.valor)}</span>
      </div>
    </li>
  );
}
