import { features } from "@/config/marketing";
import { SECTIONS } from "@/lib/routes";
import { Container } from "./container";
import { FeatureCard } from "./feature-card";
import { SectionHeading } from "./section-heading";

export function FeaturesSection() {
  return (
    <section id={SECTIONS.features} className="scroll-mt-20 py-24 sm:py-32">
      <Container>
        <SectionHeading
          eyebrow="Benefícios"
          title={
            <>
              Tudo o que você precisa,{" "}
              <span className="text-gradient-brand">nada que você não precisa.</span>
            </>
          }
          description="Uma conta digital completa, simples de usar e transparente em cada detalhe."
        />

        <div className="mt-16 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => (
            <FeatureCard key={feature.title} feature={feature} />
          ))}
        </div>
      </Container>
    </section>
  );
}
