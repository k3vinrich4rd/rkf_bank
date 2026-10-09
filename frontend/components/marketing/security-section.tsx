import { securityItems } from "@/config/marketing";
import { SECTIONS } from "@/lib/routes";
import { Container } from "./container";
import { FeatureCard } from "./feature-card";
import { SectionHeading } from "./section-heading";

export function SecuritySection() {
  return (
    <section
      id={SECTIONS.security}
      className="relative isolate scroll-mt-20 border-y border-border bg-surface/30 py-24 sm:py-32"
    >
      <div
        aria-hidden="true"
        className="absolute inset-0 -z-10 bg-grid [mask-image:radial-gradient(ellipse_at_center,black_20%,transparent_70%)]"
      />
      <Container>
        <SectionHeading
          eyebrow="Segurança"
          title="Seu dinheiro protegido em cada etapa"
          description="Segurança não é um recurso extra: é a base sobre a qual a RKF foi construída."
        />
        <div className="mt-16 grid gap-5 md:grid-cols-3">
          {securityItems.map((item) => (
            <FeatureCard key={item.title} feature={item} />
          ))}
        </div>
      </Container>
    </section>
  );
}
