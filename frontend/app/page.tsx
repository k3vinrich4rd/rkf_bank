import { CardSection } from "@/components/marketing/card-section";
import { CtaSection } from "@/components/marketing/cta-section";
import { FaqSection } from "@/components/marketing/faq-section";
import { FeaturesSection } from "@/components/marketing/features-section";
import { Hero } from "@/components/marketing/hero";
import { Highlights } from "@/components/marketing/highlights";
import { HowItWorksSection } from "@/components/marketing/how-it-works-section";
import { SecuritySection } from "@/components/marketing/security-section";
import { SiteFooter } from "@/components/marketing/site-footer";
import { SiteHeader } from "@/components/marketing/site-header";

/** Landing page institucional (estática, renderizada no build). */
export default function HomePage() {
  return (
    <>
      <SiteHeader />
      <main className="flex-1">
        <Hero />
        <Highlights />
        <FeaturesSection />
        <HowItWorksSection />
        <CardSection />
        <SecuritySection />
        <FaqSection />
        <CtaSection />
      </main>
      <SiteFooter />
    </>
  );
}
