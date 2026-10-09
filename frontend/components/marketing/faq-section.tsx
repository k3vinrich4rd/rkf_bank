import { ChevronDown } from "lucide-react";
import { faqItems } from "@/config/marketing";
import { SECTIONS } from "@/lib/routes";
import { Container } from "./container";
import { SectionHeading } from "./section-heading";

/**
 * Acordeão com <details>/<summary>: acessível por padrão e sem JavaScript
 * no cliente — continua sendo Server Component.
 */
export function FaqSection() {
  return (
    <section id={SECTIONS.faq} className="scroll-mt-20 py-24 sm:py-32">
      <Container className="max-w-3xl">
        <SectionHeading eyebrow="Dúvidas frequentes" title="Perguntas? A gente responde." />

        <div className="mt-12 divide-y divide-border rounded-2xl border border-border bg-surface/60">
          {faqItems.map((item) => (
            <details key={item.question} name="faq" className="group px-6">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-5 text-left font-medium transition-colors hover:text-brand-cyan [&::-webkit-details-marker]:hidden">
                {item.question}
                <ChevronDown
                  className="size-5 shrink-0 text-muted-foreground transition-transform duration-300 group-open:rotate-180"
                  aria-hidden="true"
                />
              </summary>
              <p className="pb-5 text-sm leading-relaxed text-muted-foreground">
                {item.answer}
              </p>
            </details>
          ))}
        </div>
      </Container>
    </section>
  );
}
