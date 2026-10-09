import Link from "next/link";
import { ArrowRight, Check, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ROUTES, SECTIONS, sectionHref } from "@/lib/routes";
import { AppPreview } from "./app-preview";
import { BankCard } from "./bank-card";
import { Container } from "./container";

const trustPoints = ["Sem mensalidade", "Sem burocracia", "Transferências 24/7"] as const;

export function Hero() {
  return (
    <section className="relative isolate overflow-hidden">
      <HeroBackground />

      <Container className="grid items-center gap-16 pt-16 pb-24 sm:pt-24 lg:grid-cols-[1.15fr_1fr] lg:pt-28 lg:pb-32">
        <div className="flex flex-col items-start animate-in fade-in slide-in-from-bottom-4 duration-700">
          <span className="inline-flex items-center gap-2 rounded-full border border-brand-blue/30 bg-brand-blue/10 px-3 py-1 text-xs font-medium text-brand-cyan">
            <Sparkles className="size-3.5" aria-hidden="true" />
            Banco 100% digital
          </span>

          <h1 className="mt-6 text-4xl font-semibold tracking-tight sm:text-5xl lg:text-[3.4rem] lg:leading-[1.08] xl:text-6xl">
            <span className="block">Mais que um banco.</span>
            <span className="block">Um parceiro</span>
            <span className="block text-gradient-brand">para o seu amanhã.</span>
          </h1>

          <p className="mt-6 max-w-xl text-lg text-pretty text-muted-foreground">
            Abra sua conta em minutos, transfira na hora e acompanhe cada centavo
            em tempo real. Tecnologia de ponta a serviço da sua liberdade
            financeira.
          </p>

          <div className="mt-10 flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
            <Button asChild variant="brand" size="xl">
              <Link href={ROUTES.signUp}>
                Abrir minha conta
                <ArrowRight data-icon="inline-end" />
              </Link>
            </Button>
            <Button asChild variant="outline" size="xl">
              <a href={sectionHref(SECTIONS.features)}>Conhecer benefícios</a>
            </Button>
          </div>

          <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {trustPoints.map((point) => (
              <li key={point} className="flex items-center gap-2">
                <Check className="size-4 text-brand-cyan" aria-hidden="true" />
                {point}
              </li>
            ))}
          </ul>
        </div>

        <div className="relative mx-auto w-full max-w-md animate-in fade-in slide-in-from-bottom-8 duration-1000 lg:max-w-none">
          {/* O cartão fica atrás do app, "espiando" pela lateral, sem cobrir conteúdo. */}
          <BankCard className="absolute top-8 -left-2 z-0 max-w-[15rem] -rotate-12 sm:-left-6 sm:max-w-[18rem] lg:-left-10" />
          <AppPreview className="relative z-10 ml-auto max-w-[19rem] sm:max-w-sm" />
        </div>
      </Container>
    </section>
  );
}

function HeroBackground() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
      <div className="absolute inset-0 bg-grid [mask-image:radial-gradient(ellipse_at_top,black_30%,transparent_75%)]" />
      <div className="absolute -top-40 left-1/2 size-[42rem] -translate-x-1/2 rounded-full bg-brand-blue/20 blur-[120px]" />
      <div className="absolute top-1/3 -right-40 size-[28rem] rounded-full bg-brand-cyan/15 blur-[120px]" />
      <svg
        viewBox="0 0 1440 320"
        preserveAspectRatio="none"
        className="absolute inset-x-0 bottom-0 h-40 w-full"
      >
        <path
          d="M0 260 L180 170 L320 230 L520 110 L700 210 L880 130 L1060 220 L1260 120 L1440 200 V320 H0Z"
          fill="url(#ridge)"
          opacity=".5"
        />
        <defs>
          <linearGradient id="ridge" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#111827" />
            <stop offset="1" stopColor="#0a0f1c" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  );
}
