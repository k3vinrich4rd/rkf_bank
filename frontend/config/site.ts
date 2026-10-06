import { SECTIONS, sectionHref } from "@/lib/routes";

/** Metadados globais da marca, reaproveitados em SEO, header e footer. */
export const siteConfig = {
  name: "RKF",
  fullName: "RKF Banco Digital",
  tagline: "Confiança. Tecnologia. Liberdade.",
  description:
    "RKF Banco Digital: conta digital sem mensalidade, transferências na hora e extrato em tempo real. Mais que um banco, um parceiro para o seu amanhã.",
  locale: "pt_BR",
} as const;

export interface NavItem {
  label: string;
  href: string;
}

export const mainNav: readonly NavItem[] = [
  { label: "Benefícios", href: sectionHref(SECTIONS.features) },
  { label: "Como funciona", href: sectionHref(SECTIONS.howItWorks) },
  { label: "Cartão", href: sectionHref(SECTIONS.card) },
  { label: "Segurança", href: sectionHref(SECTIONS.security) },
  { label: "Dúvidas", href: sectionHref(SECTIONS.faq) },
];
