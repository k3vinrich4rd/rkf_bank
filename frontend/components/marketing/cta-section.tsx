import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { Container } from "./container";

export function CtaSection() {
  return (
    <section className="pb-24 sm:pb-32">
      <Container>
        <div className="relative isolate overflow-hidden rounded-3xl border border-brand-blue/30 bg-[linear-gradient(135deg,#0f1e3d,#0a0f1c_60%,#08303f)] px-6 py-16 text-center sm:px-16 sm:py-20">
          <div
            aria-hidden="true"
            className="absolute -top-24 left-1/2 -z-10 size-96 -translate-x-1/2 rounded-full bg-brand-blue/30 blur-[100px]"
          />
          <h2 className="mx-auto max-w-2xl text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
            Pronto para ter um banco que trabalha{" "}
            <span className="text-gradient-brand">a seu favor?</span>
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-muted-foreground">
            Abra sua conta gratuita agora e descubra uma nova forma de cuidar do seu dinheiro.
          </p>
          <Button asChild variant="brand" size="xl" className="mt-10">
            <Link href={ROUTES.signUp}>
              Abrir minha conta grátis
              <ArrowRight data-icon="inline-end" />
            </Link>
          </Button>
        </div>
      </Container>
    </section>
  );
}
