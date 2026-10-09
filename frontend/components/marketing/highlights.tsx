import { highlights } from "@/config/marketing";
import { Container } from "./container";

export function Highlights() {
  return (
    <section aria-label="Destaques" className="border-y border-border bg-surface/40">
      <Container>
        <dl className="grid grid-cols-2 divide-border py-10 md:grid-cols-4 md:divide-x">
          {highlights.map((item) => (
            // flex-col-reverse: leitura semântica "rótulo → valor", exibição "valor → rótulo".
            <div key={item.label} className="flex flex-col-reverse items-center gap-1 px-4 py-4 text-center">
              <dt className="text-sm text-muted-foreground">{item.label}</dt>
              <dd className="text-3xl font-semibold tracking-tight text-gradient-brand sm:text-4xl">
                {item.value}
              </dd>
            </div>
          ))}
        </dl>
      </Container>
    </section>
  );
}
