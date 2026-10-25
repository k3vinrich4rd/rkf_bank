"use client";

import { useEffect, useRef } from "react";
import { criarCartao, PROPORCAO_CARTAO, type Cartao } from "@/lib/motion/cartao-3d";
import { estadoInicialDoCartao } from "@/lib/motion/estado-inicial";
import { gsap, querMovimentoReduzido, ScrollTrigger } from "@/lib/motion/gsap";
import { CelularApp } from "./celular-app";
import { APP, FINAL_CARTAO, HERO, TITULAR, VERSO } from "./conteudo";
import { useAjustarPalavra } from "./use-ajustar-palavra";

/** "RKF" nunca passa de 50% da altura da tela (telas largas e baixas). */
const alturaDaPalavra = () => window.innerHeight * 0.5;

type Pose = { cx: number; cy: number; len: number };
interface Layout {
  hero: Pose;
  verso: Pose;
  encaixe: Pose;
}

/** Quantas alturas de tela o palco fica preso ao scroll. */
const ALTURAS_PRESO = 3.4;
const GRAUS = Math.PI / 180;

/**
 * Hero com o cartão RKF em 3D, preso ao scroll: o cartão em pé na frente do "K" → vira e mostra o verso →
 * desce, gira de novo e encaixa na vaga do app. As poses são funções do tamanho do palco (medidas no DOM),
 * refeitas a cada `refresh` do ScrollTrigger, então o encaixe continua exato em qualquer tela.
 */
export function HeroCartao() {
  const palco = useRef<HTMLDivElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);
  const palavra = useRef<HTMLSpanElement>(null);
  const letraK = useRef<HTMLSpanElement>(null);
  const celular = useRef<HTMLDivElement>(null);
  const vaga = useRef<HTMLDivElement>(null);
  const textosHero = useRef<HTMLDivElement>(null);
  const textoVerso = useRef<HTMLDivElement>(null);
  const textoApp = useRef<HTMLDivElement>(null);
  const porcentagem = useRef<HTMLSpanElement>(null);

  useAjustarPalavra(palavra, alturaDaPalavra);

  useEffect(() => {
    const elementos = {
      palco: palco.current,
      canvas: canvas.current,
      palavra: palavra.current,
      letraK: letraK.current,
      celular: celular.current,
      vaga: vaga.current,
      textosHero: textosHero.current,
      textoVerso: textoVerso.current,
      textoApp: textoApp.current,
    };
    if (Object.values(elementos).some((elemento) => !elemento)) return;
    const el = elementos as { [K in keyof typeof elementos]: NonNullable<(typeof elementos)[K]> };

    const reduzido = querMovimentoReduzido();
    const estado = estadoInicialDoCartao();
    let layout = medir(el);
    const aoAtualizar = () => {
      layout = medir(el);
    };
    ScrollTrigger.addEventListener("refreshInit", aoAtualizar);

    const ctx = gsap.context(() => {
      const timeline = gsap.timeline({
        defaults: { ease: "none" },
        scrollTrigger: {
          trigger: el.palco,
          start: "top top",
          end: () => `+=${window.innerHeight * ALTURAS_PRESO}`,
          pin: true,
          scrub: reduzido ? true : 0.8,
          invalidateOnRefresh: true,
        },
      });
      const itensApp = el.celular.querySelectorAll(".app-item");

      if (reduzido) {
        // Sem giro: o hero dá lugar à tela do app num corte suave.
        gsap.set(estado, { ...poseFinal(() => layout), tilt: 0, shadow: 0 });
        timeline
          .set(estado, poseHero(() => layout), 0)
          .to([el.textosHero, el.palavra, el.canvas], { autoAlpha: 0, duration: 1 }, 0)
          .set(estado, poseFinal(() => layout), 1)
          .set(el.celular, { y: 0 }, 1)
          .to([el.celular, el.canvas], { autoAlpha: 1, duration: 1 }, 1.2)
          .fromTo(itensApp, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 1.4)
          .fromTo(el.textoApp.children, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 1.4)
          .to({}, { duration: 1 });
        gsap.set(estado, poseHero(() => layout));
        return;
      }

      timeline
        // 1. Os textos do hero saem.
        .to(el.textosHero.children, { autoAlpha: 0, y: -24, stagger: 0.08, duration: 0.9 }, 0.15)
        // 2. Vira: o cartão se endireita, cresce e mostra o verso; a palavra escurece.
        .fromTo(
          estado,
          { ...poseHero(() => layout), env: 0, shadow: 0.42, tilt: 1, light: -0.3 },
          { ...poseVerso(() => layout), env: 0.9, shadow: 0.3, tilt: 0.35, light: 0.35, duration: 2.6, ease: "power2.inOut" },
          0.4,
        )
        .to(el.palavra, { opacity: 0.16, duration: 2.4 }, 0.4)
        .fromTo(el.textoVerso.children, { autoAlpha: 0, y: 28 }, { autoAlpha: 1, y: 0, stagger: 0.12, duration: 0.8 }, 2.5)
        .to(el.textoVerso.children, { autoAlpha: 0, y: -20, stagger: 0.05, duration: 0.7 }, 4.2)
        // 3. Encaixa: o celular sobe aparecendo aos poucos (nunca opaco desde o início: as bordas dele
        //    surgiriam sozinhas embaixo do cartão) e o cartão gira de volta, deita e entra na vaga.
        .fromTo(
          el.celular,
          { y: () => window.innerHeight * 0.85, autoAlpha: 0 },
          { y: 0, autoAlpha: 1, duration: 2.8, ease: "power2.out" },
          4.4,
        )
        .fromTo(
          estado,
          { ...poseVerso(() => layout), env: 0.9, shadow: 0.3, tilt: 0.35, light: 0.35 },
          { ...poseFinal(() => layout), env: 1.6, shadow: 0, tilt: 0, light: 0.6, duration: 3, ease: "power3.inOut" },
          4.4,
        )
        .to(el.palavra, { opacity: 0, duration: 1.6 }, 4.8)
        // 4. App: a tela se completa em volta do cartão e o texto do lado entra.
        .fromTo(itensApp, { autoAlpha: 0, y: 10 }, { autoAlpha: 1, y: 0, stagger: 0.06, duration: 0.5 }, 7.1)
        .fromTo(el.textoApp.children, { autoAlpha: 0, y: 24 }, { autoAlpha: 1, y: 0, stagger: 0.1, duration: 0.7 }, 7.3)
        .to({}, { duration: 1.4 });
    }, el.palco);

    // "Role 0%": a porcentagem real da página inteira.
    const rolagem = ScrollTrigger.create({
      start: 0,
      end: "max",
      onUpdate: ({ progress }) => {
        if (porcentagem.current) porcentagem.current.textContent = `${Math.round(progress * 100)}%`;
      },
    });

    // O cartão nasce depois das fontes: o nome e o número são desenhados com elas na textura.
    let cartao: Cartao | null = null;
    let cancelado = false;
    document.fonts.ready.then(() => {
      if (cancelado) return;
      cartao = criarCartao({
        canvas: el.canvas,
        palco: el.palco,
        reduzido,
        titular: TITULAR,
        final: FINAL_CARTAO,
        fontes: lerFontes(el.palco),
        estado,
      });
      gsap.ticker.add(cartao.render);
    });

    const aoRedimensionar = () => cartao?.resize();
    window.addEventListener("resize", aoRedimensionar);

    return () => {
      cancelado = true;
      window.removeEventListener("resize", aoRedimensionar);
      ScrollTrigger.removeEventListener("refreshInit", aoAtualizar);
      rolagem.kill();
      ctx.revert();
      if (cartao) {
        gsap.ticker.remove(cartao.render);
        cartao.destruir();
      }
    };
  }, []);

  return (
    <section id="conta" aria-labelledby="hero-titulo" className="relative">
      <div ref={palco} className="relative h-svh overflow-hidden">
        {/* 1. A palavra gigante, de margem a margem. */}
        <div aria-hidden="true" className="absolute inset-x-0 top-[38%] -translate-y-1/2 px-4 text-center sm:top-[44%]">
          <span ref={palavra} className="palavra fonte-larga text-[var(--l-palavra)]">
            R<span ref={letraK}>K</span>F
          </span>
        </div>

        {/* 2. O celular, escondido embaixo até a hora do encaixe. */}
        <CelularApp ref={celular} vagaRef={vaga} />

        {/* 3. O cartão 3D, num canvas transparente por cima de tudo. */}
        <canvas ref={canvas} aria-hidden="true" className="pointer-events-none absolute inset-0 z-10 size-full" />

        {/* 4. Textos. */}
        <div ref={textosHero} className="absolute inset-x-0 bottom-0 z-20 px-4 pb-6 sm:px-10 sm:pb-10">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
            <h1 id="hero-titulo" className="max-w-xl text-3xl font-semibold tracking-tight text-balance sm:text-5xl">
              {HERO.titulo[0]}
              <span className="block text-[var(--l-suave)]">{HERO.titulo[1]}</span>
            </h1>
            <ul className="fonte-mono w-full text-xs text-[var(--l-suave)] sm:w-72">
              {HERO.specs.map((spec) => (
                <li key={spec} className="border-t border-[var(--l-fio)] py-2 last:border-b">
                  {spec}
                </li>
              ))}
            </ul>
          </div>
          <p className="fonte-mono mt-6 text-center text-[0.7rem] tracking-[0.2em] text-[var(--l-suave)] uppercase">
            Role <span ref={porcentagem}>0%</span>
          </p>
        </div>

        <div
          ref={textoVerso}
          className="entra-depois absolute inset-x-0 bottom-0 z-20 px-4 pb-10 sm:inset-x-auto sm:top-1/2 sm:bottom-auto sm:left-10 sm:max-w-sm sm:-translate-y-1/2 sm:pb-0"
        >
          <p className="fonte-mono flex items-center gap-2 text-xs text-[var(--l-suave)]">
            <span className="ponto-acento" />
            {VERSO.rotulo}
          </p>
          <h2 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">
            {VERSO.titulo[0]}
            <span className="block text-[var(--l-suave)]">{VERSO.titulo[1]}</span>
          </h2>
          <ul className="fonte-mono mt-6 text-xs text-[var(--l-suave)]">
            {VERSO.specs.map((spec) => (
              <li key={spec} className="border-t border-[var(--l-fio)] py-2 last:border-b">
                {spec}
              </li>
            ))}
          </ul>
        </div>

        <div
          ref={textoApp}
          className="entra-depois absolute inset-x-0 top-0 z-20 px-4 pt-24 sm:inset-x-auto sm:top-1/2 sm:left-10 sm:max-w-sm sm:-translate-y-1/2 sm:pt-0 lg:left-[8%]"
        >
          <p className="fonte-mono text-xs text-[var(--l-suave)]">{APP.rotulo}</p>
          <h2 className="mt-3 text-2xl font-semibold tracking-tight sm:mt-4 sm:text-4xl">
            {APP.titulo[0]}
            <span className="block text-[var(--l-suave)]">{APP.titulo[1]}</span>
          </h2>
          <ul className="fonte-mono mt-6 hidden text-xs text-[var(--l-suave)] sm:block">
            {APP.specs.map((spec) => (
              <li key={spec} className="border-t border-[var(--l-fio)] py-2 last:border-b">
                {spec}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

interface Elementos {
  palco: HTMLDivElement;
  letraK: HTMLSpanElement;
  celular: HTMLDivElement;
  vaga: HTMLDivElement;
}

/** As três poses do cartão, em px do palco. */
function medir({ palco, letraK, celular, vaga }: Elementos): Layout {
  const p = palco.getBoundingClientRect();
  const w = p.width;
  const h = p.height;
  const celularLargo = w >= 768;

  const k = letraK.getBoundingClientRect();
  const hero: Pose = {
    cx: k.left - p.left + k.width / 2,
    cy: k.top - p.top + k.height * 0.5,
    len: celularLargo ? Math.min(w * 0.24, h * 0.46) : w * 0.48,
  };

  const verso: Pose = celularLargo
    ? { cx: w * 0.62, cy: h * 0.5, len: Math.min(w * 0.46, h * 0.56 * PROPORCAO_CARTAO) }
    : { cx: w / 2, cy: h * 0.36, len: w * 0.86 };

  // A vaga é medida com o celular na posição final: o `transform` da animação sai por um instante.
  const transformAnterior = celular.style.transform;
  celular.style.transform = "none";
  const v = vaga.getBoundingClientRect();
  celular.style.transform = transformAnterior;
  const encaixe: Pose = { cx: v.left - p.left + v.width / 2, cy: v.top - p.top + v.height / 2, len: v.width };

  return { hero, verso, encaixe };
}

/** As poses viram valores-função: o GSAP as reavalia a cada `refresh` (`invalidateOnRefresh`). */
function poseHero(layout: () => Layout) {
  return {
    cx: () => layout().hero.cx,
    cy: () => layout().hero.cy,
    len: () => layout().hero.len,
    rx: 0.12,
    ry: -0.32,
    rz: 53 * GRAUS,
  };
}

function poseVerso(layout: () => Layout) {
  return {
    cx: () => layout().verso.cx,
    cy: () => layout().verso.cy,
    len: () => layout().verso.len,
    rx: -0.08,
    ry: Math.PI - 0.18,
    rz: 0,
  };
}

function poseFinal(layout: () => Layout) {
  return {
    cx: () => layout().encaixe.cx,
    cy: () => layout().encaixe.cy,
    len: () => layout().encaixe.len,
    rx: 0,
    ry: Math.PI * 2,
    rz: 0,
  };
}

/** As famílias reais das fontes (o `next/font` gera nomes com hash), para o canvas do cartão usar as mesmas. */
function lerFontes(elemento: HTMLElement) {
  const estilo = getComputedStyle(elemento);
  const larga = estilo.getPropertyValue("--font-larga").trim() || "sans-serif";
  const mono = estilo.getPropertyValue("--font-mono-landing").trim() || "monospace";
  const texto = estilo.getPropertyValue("--font-plus-jakarta-sans").trim() || "sans-serif";
  return { texto, larga, mono };
}
