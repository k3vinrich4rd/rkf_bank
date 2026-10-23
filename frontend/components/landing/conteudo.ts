/**
 * Textos da landing num lugar só. Regra: **só prometer o que o banco faz hoje** (contrato da API).
 * O que ainda não existe aparece marcado como "em breve".
 */

export const NAV = [
  { rotulo: "Conta", href: "#conta" },
  { rotulo: "Extrato", href: "#extrato" },
  { rotulo: "Segurança", href: "#seguranca" },
  { rotulo: "Cartão", href: "#contas" },
  { rotulo: "Sobre", href: "#sobre" },
] as const;

export const HERO = {
  titulo: ["O banco que mostra", "cada centavo."],
  specs: ["Conta aberta em minutos", "Endereço pelo CEP", "Transferência na hora"],
};

export const VERSO = {
  rotulo: "Cartão RKF · em breve",
  titulo: ["Seu nome na frente.", "O número só no verso."],
  specs: ["Débito com chip e aproximação", "Número completo só no verso e no app", "Bloqueio pelo app"],
};

/** O mesmo saldo aparece no app do hero e no fim do extrato: a página conta uma história só. */
export const SALDO_FINAL = 4820.5;
export const TITULAR = "Fellipe Santana";
export const FINAL_CARTAO = "0731";

export const APP = {
  rotulo: "01 — Conta",
  titulo: ["Saldo, extrato e transferência.", "Numa tela só."],
  specs: ["Corrente ou poupança", "Agência 0001 · número com dígito", "Sessão que expira sozinha"],
  conta: "Conta corrente · Ag 0001 · 4821509-3",
  acoes: ["Depositar", "Sacar", "Transferir", "Extrato"],
};

export interface Lancamento {
  hora: string;
  descricao: string;
  valor: number;
}

/** Lançamentos de ontem: já estão na tela quando a seção começa. */
export const EXTRATO_ANTIGO: readonly Lancamento[] = [
  { hora: "Ontem 19:22", descricao: "Depósito", valor: 500 },
  { hora: "Ontem 12:10", descricao: "Saque", valor: -80 },
  { hora: "Ontem 08:45", descricao: "Transferência recebida", valor: 900 },
];

/** Lançamentos de hoje, do mais antigo ao mais novo: entram com o scroll e o saldo chega em `SALDO_FINAL`. */
export const EXTRATO_NOVO: readonly Lancamento[] = [
  { hora: "09:14", descricao: "Depósito", valor: 350 },
  { hora: "11:02", descricao: "Transferência · Ana", valor: -120 },
  { hora: "13:40", descricao: "Saque", valor: -200 },
  { hora: "16:25", descricao: "Transferência recebida", valor: 1200 },
  { hora: "18:03", descricao: "Depósito", valor: 470.1 },
];

export const SALDO_INICIAL = EXTRATO_NOVO.reduce((saldo, { valor }) => saldo - valor, SALDO_FINAL);

export const UMA_VEZ = {
  rotulo: "03 — Segurança",
  titulo: ["Clicou duas vezes?", "Cobramos uma."],
  tentativas: [
    { hora: "14:02:11", acao: "Envio", resultado: "Transferido · R$ 250,00" },
    { hora: "14:02:12", acao: "Reenvio · a rede caiu", resultado: "Já feito · nada cobrado" },
    { hora: "14:02:14", acao: "Reenvio · clique duplo", resultado: "Já feito · nada cobrado" },
  ],
  nota: "Cada operação leva uma chave única. Repetir a mesma chave não movimenta de novo.",
  specs: ["Sessão expira em 15 min", "Senha guardada só como hash", "5 senhas erradas · pausa de 15 min"],
};

export const CEP = {
  rotulo: "04 — Cadastro",
  titulo: ["Digite o CEP.", "O endereço vem sozinho."],
  cep: "01001-000",
  endereco: "Praça da Sé · Sé · São Paulo – SP",
  marcos: ["Seus dados", "CEP", "Senha", "Conta aberta"],
};

export interface Pessoa {
  nome: string;
  papel: string;
  faz: string;
  github?: { usuario: string; url: string };
}

export const TIME = {
  rotulo: "05 — Sobre",
  titulo: ["Três amigos da mesma rua.", "Um banco feito a seis mãos."],
  historia: [
    "Renan, Kevin e Fellipe moram na mesma rua, em Osasco, desde que nasceram. Cresceram juntos e, um dia, decidiram construir algo sério juntos.",
    "O RKF é esse projeto: um banco digital completo, do cadastro ao extrato, feito com o cuidado de quem usa o próprio produto.",
  ],
  pessoas: [
    {
      nome: "Renan Barros",
      papel: "Desenvolvedor frontend",
      faz: "Telas, sessão segura e logs",
      github: { usuario: "renanbarrostech-cmd", url: "https://github.com/renanbarrostech-cmd" },
    },
    {
      nome: "Kevin",
      papel: "Desenvolvedor backend",
      faz: "API, segurança e banco de dados",
      github: { usuario: "k3vinrich4rd", url: "https://github.com/k3vinrich4rd" },
    },
    {
      nome: "Fellipe Santana",
      papel: "Testes e produto",
      faz: "Testa cada versão antes de ir ao ar · o primeiro cliente",
    },
  ] satisfies Pessoa[],
};

export const CONTAS = {
  rotulo: "06 — Contas",
  titulo: ["Duas contas hoje.", "Um cartão logo mais."],
  planos: [
    {
      nome: "Corrente",
      preco: "R$ 0",
      sufixo: "/mês",
      itens: ["Depósito, saque e transferência", "Extrato agrupado por dia", "Número com dígito verificador"],
    },
    {
      nome: "Poupança",
      preco: "R$ 0",
      sufixo: "/mês",
      itens: ["As mesmas movimentações", "Separada da corrente", "Mais de uma conta por cliente"],
    },
    {
      nome: "Cartão RKF",
      preco: "Em breve",
      sufixo: "",
      destaque: true,
      itens: ["Débito com chip e aproximação", "Seu nome na frente", "Bloqueio pelo app"],
    },
  ],
};

export const RODAPE = {
  titulo: ["Abrir conta leva poucos minutos.", "O resto aparece no extrato."],
  linha: "RKF Banco Digital · Osasco, SP · 2026",
  aviso: "Projeto de portfólio: não é instituição autorizada pelo Banco Central.",
};

const REAIS = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatarReais(valor: number) {
  return REAIS.format(valor);
}

/** "+R$ 350,00" / "−R$ 120,00", com o sinal de menos tipográfico. */
export function formatarLancamento(valor: number) {
  return `${valor < 0 ? "−" : "+"}${REAIS.format(Math.abs(valor))}`;
}

/** O app do hero mostra os três últimos lançamentos do extrato, do mais novo ao mais antigo. */
export const APP_LANCAMENTOS = EXTRATO_NOVO.slice(-3).reverse();
