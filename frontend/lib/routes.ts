/**
 * Fonte única das rotas da aplicação. Evita strings mágicas espalhadas
 * pelos componentes e facilita renomear uma rota no futuro.
 */
export const ROUTES = {
  home: "/",
  login: "/login",
  /** Abertura de conta ainda não existe no frontend; aponta para o login. */
  signUp: "/login",
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
