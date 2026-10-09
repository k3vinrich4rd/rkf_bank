import { Check } from "lucide-react";
import { SECTIONS } from "@/lib/routes";
import { BankCard } from "./bank-card";
import { Container } from "./container";
import { SectionHeading } from "./section-heading";

const cardPerks = [
  "Sem anuidade",
  "Aproximação (contactless)",
  "Controle total pelo app",
  "Bloqueio e desbloqueio instantâneos",
] as const;

export function CardSection() {
  return (
    <section id={SECTIONS.card} className="scroll-mt-20 overflow-hidden py-24 sm:py-32">
      <Container className="grid items-center gap-16 lg:grid-cols-2">
        <div className="relative isolate mx-auto w-full max-w-md pt-10 pr-10">
          <div
            aria-hidden="true"
            className="absolute inset-0 -z-10 scale-110 rounded-full bg-brand-blue/20 blur-[100px]"
          />
          <BankCard className="absolute top-0 right-0 w-[88%] rotate-6 opacity-50" />
          <BankCard holder="Renan Barros" lastDigits="0731" className="relative w-[88%] -rotate-3" />
        </div>

        <div>
          <SectionHeading
            align="left"
            eyebrow="Cartão RKF · em breve"
            title={
              <>
                Um cartão à altura{" "}
                <span className="text-gradient-brand">da sua ambição.</span>
              </>
            }
            description="Design premium, segurança de ponta e controle na palma da mão. Seja um dos primeiros a ter o seu."
          />
          <ul className="mt-8 grid gap-3 sm:grid-cols-2">
            {cardPerks.map((perk) => (
              <li key={perk} className="flex items-center gap-3 text-sm">
                <span className="grid size-6 place-items-center rounded-full bg-brand-cyan/10 text-brand-cyan">
                  <Check className="size-3.5" aria-hidden="true" />
                </span>
                {perk}
              </li>
            ))}
          </ul>
        </div>
      </Container>
    </section>
  );
}
