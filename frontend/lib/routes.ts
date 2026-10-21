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
  trocarSenha: "/conta/senha",
} as const;

/**
 * Para onde ir depois do login (`?voltar=`). Só aceita caminhos internos: `//site.com` ou `https://…`
 * transformariam o link de login num redirecionamento para fora (open redirect).
 */
export function destinoDepoisDoLogin(voltar: string | undefined): string {
  if (!voltar?.startsWith("/") || voltar.startsWith("//") || voltar.startsWith("/\\")) return ROUTES.conta;
  return voltar;
}

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
