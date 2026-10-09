import {
  ArrowLeftRight,
  Fingerprint,
  Landmark,
  LockKeyhole,
  MapPin,
  Receipt,
  ShieldCheck,
  Smartphone,
  Wallet,
  type LucideIcon,
} from "lucide-react";

/**
 * Conteúdo da landing page separado da apresentação: o time de produto
 * edita textos aqui sem tocar nos componentes.
 *
 * Os benefícios refletem o escopo real do MVP (ver planing/roadmap.md) —
 * não prometa no marketing o que a API ainda não entrega.
 */

export interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

export interface Highlight {
  value: string;
  label: string;
}

export interface Step {
  title: string;
  description: string;
}

export interface FaqItem {
  question: string;
  answer: string;
}

export const highlights: readonly Highlight[] = [
  { value: "R$ 0", label: "de mensalidade" },
  { value: "24/7", label: "transferências entre contas" },
  { value: "100%", label: "digital, do cadastro ao extrato" },
  { value: "< 5 min", label: "para abrir sua conta" },
];

export const features: readonly Feature[] = [
  {
    icon: Wallet,
    title: "Conta corrente e poupança",
    description:
      "Escolha o tipo de conta ideal para você e acompanhe seu saldo sempre atualizado.",
  },
  {
    icon: ArrowLeftRight,
    title: "Transferências na hora",
    description:
      "Envie dinheiro entre contas RKF em segundos, com confirmação instantânea.",
  },
  {
    icon: Receipt,
    title: "Extrato em tempo real",
    description:
      "Cada depósito, saque e transferência aparece no seu extrato no momento em que acontece.",
  },
  {
    icon: MapPin,
    title: "Cadastro inteligente",
    description:
      "Informe só o CEP e nós completamos seu endereço automaticamente.",
  },
  {
    icon: Landmark,
    title: "Depósitos e saques",
    description:
      "Movimente seu dinheiro com regras claras e sem tarifas escondidas.",
  },
  {
    icon: Smartphone,
    title: "Tudo pelo celular",
    description:
      "Uma experiência pensada para a tela que está sempre com você.",
  },
];

export const steps: readonly Step[] = [
  {
    title: "Faça seu cadastro",
    description:
      "Nome, CPF, e-mail, telefone e CEP. Validamos seus dados na hora.",
  },
  {
    title: "Abra sua conta",
    description:
      "Escolha entre conta corrente ou poupança. Pronto: sua agência e conta já estão ativas.",
  },
  {
    title: "Movimente com liberdade",
    description:
      "Deposite, saque e transfira. Acompanhe tudo pelo extrato em tempo real.",
  },
];

export const securityItems: readonly Feature[] = [
  {
    icon: ShieldCheck,
    title: "Dados validados",
    description:
      "CPF, e-mail e endereço são verificados antes de qualquer conta ser criada.",
  },
  {
    icon: LockKeyhole,
    title: "Operações atômicas",
    description:
      "Toda transferência debita e credita na mesma transação: ou acontece por inteiro, ou não acontece.",
  },
  {
    icon: Fingerprint,
    title: "Acesso protegido",
    description:
      "Login seguro com sessão protegida e, em breve, autenticação em dois fatores.",
  },
];

export const faqItems: readonly FaqItem[] = [
  {
    question: "A conta RKF tem mensalidade?",
    answer:
      "Não. A conta digital RKF não cobra mensalidade nem tarifa de manutenção.",
  },
  {
    question: "Quais documentos preciso para abrir minha conta?",
    answer:
      "Apenas seus dados pessoais: nome completo, CPF, e-mail, telefone e CEP. O endereço é preenchido automaticamente.",
  },
  {
    question: "Quanto tempo leva para a transferência cair?",
    answer:
      "Transferências entre contas RKF são processadas na hora, 24 horas por dia, 7 dias por semana.",
  },
  {
    question: "Posso ter conta corrente e poupança?",
    answer:
      "Sim. Você escolhe o tipo de conta no momento da abertura e pode acompanhar cada uma pelo extrato.",
  },
  {
    question: "Quando o cartão RKF estará disponível?",
    answer:
      "O cartão está no nosso roadmap logo após o lançamento. Abra sua conta para ser avisado em primeira mão.",
  },
];
