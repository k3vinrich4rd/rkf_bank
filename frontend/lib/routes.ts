/**
 * Fonte única das rotas da aplicação. Evita strings mágicas espalhadas
 * pelos componentes e facilita renomear uma rota no futuro.
 */
export const ROUTES = {
  home: "/",
  login: "/login",
  signUp: "/cadastro",
  conta: "/conta",
  abrirConta: "/conta/abrir",
  depositar: "/conta/depositar",
  sacar: "/conta/sacar",
  transferir: "/conta/transferir",
  extrato: "/conta/extrato",
} as const;

/** Âncoras da landing page (ids das seções). */
export const SECTIONS = {
  features: "beneficios",
  howItWorks: "como-funciona",
  card: "cartao",
  security: "seguranca",
  faq: "faq",
} as const;

export type SectionId = (typeof SECTIONS)[keyof typeof SECTIONS];

export const sectionHref = (id: SectionId) => `#${id}` as const;
