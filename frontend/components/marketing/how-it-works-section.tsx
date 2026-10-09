import { steps } from "@/config/marketing";
import { SECTIONS } from "@/lib/routes";
import { Container } from "./container";
import { SectionHeading } from "./section-heading";

export function HowItWorksSection() {
  return (
    <section
      id={SECTIONS.howItWorks}
      className="relative scroll-mt-20 border-y border-border bg-surface/30 py-24 sm:py-32"
    >
      <Container>
        <SectionHeading
          eyebrow="Como funciona"
          title="Sua conta pronta em três passos"
          description="Sem filas, sem papelada. Do cadastro à primeira transferência em poucos minutos."
        />

        <ol className="relative mt-16 grid gap-10 md:grid-cols-3 md:gap-6">
          <div
            aria-hidden="true"
            className="absolute top-6 right-[16%] left-[16%] hidden h-px bg-gradient-brand opacity-40 md:block"
          />
          {steps.map((step, index) => (
            <li key={step.title} className="relative flex flex-col items-center text-center">
              <span className="grid size-12 place-items-center rounded-full border border-brand-blue/40 bg-background text-lg font-semibold text-brand-cyan shadow-lg shadow-brand-blue/20">
                {index + 1}
              </span>
              <h3 className="mt-6 text-lg font-semibold">{step.title}</h3>
              <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted-foreground">
                {step.description}
              </p>
            </li>
          ))}
        </ol>
      </Container>
    </section>
  );
}
