import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine, type LucideIcon } from "lucide-react";

export type TipoMovimentacao = "deposito" | "saque" | "transferencia";

interface ConfigMovimentacao {
  titulo: string;
  subtitulo: string;
  rotuloBotao: string;
  rotuloEnviando: string;
  tituloSucesso: string;
  rotuloRepetir: string;
  /** Saque e transferência tiram dinheiro da conta: o valor precisa caber no saldo. */
  debita: boolean;
  icone: LucideIcon;
}

/** Textos e regras que mudam de uma movimentação para outra. O fluxo é o mesmo nas três. */
export const MOVIMENTACOES: Record<TipoMovimentacao, ConfigMovimentacao> = {
  deposito: {
    titulo: "Depositar",
    subtitulo: "O valor entra na hora e já aparece no seu saldo.",
    rotuloBotao: "Depositar",
    rotuloEnviando: "Depositando…",
    tituloSucesso: "Depósito concluído.",
    rotuloRepetir: "Fazer outro depósito",
    debita: false,
    icone: ArrowDownToLine,
  },
  saque: {
    titulo: "Sacar",
    subtitulo: "Retire dinheiro da sua conta, até o valor do seu saldo.",
    rotuloBotao: "Sacar",
    rotuloEnviando: "Sacando…",
    tituloSucesso: "Saque concluído.",
    rotuloRepetir: "Fazer outro saque",
    debita: true,
    icone: ArrowUpFromLine,
  },
  transferencia: {
    titulo: "Transferir",
    subtitulo: "Envie dinheiro para outra conta do RKF Bank usando o ID dela.",
    rotuloBotao: "Transferir",
    rotuloEnviando: "Transferindo…",
    tituloSucesso: "Transferência enviada.",
    rotuloRepetir: "Fazer outra transferência",
    debita: true,
    icone: ArrowLeftRight,
  },
};
