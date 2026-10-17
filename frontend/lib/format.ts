import type { DecimalAmount, IsoDateTime, TipoConta } from "@/lib/api";
import type { Cents } from "@/types";

const NOMES_TIPO_CONTA: Record<TipoConta, string> = {
  CORRENTE: "Conta corrente",
  POUPANCA: "Poupança",
};

/** CORRENTE → "Conta corrente" · POUPANCA → "Poupança" */
export function formatTipoConta(tipo: TipoConta): string {
  return NOMES_TIPO_CONTA[tipo];
}

const brlFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/**
 * Formata um valor monetário em BRL.
 *
 * O frontend faz contas em centavos (inteiro) para evitar erro de ponto
 * flutuante; a API trafega decimal em reais (ver `decimalToCents`) e a
 * conversão para texto acontece só na borda de exibição.
 *
 * @param cents valor em centavos, ex.: 123456
 * @returns string formatada, ex.: "R$ 1.234,56"
 */
export function formatCurrency(cents: Cents): string {
  if (!Number.isFinite(cents)) {
    throw new TypeError(
      `formatCurrency espera um número finito em centavos, recebeu: ${cents}`,
    );
  }

  return brlFormatter.format(cents / 100);
}

/**
 * Converte um valor decimal em reais (formato atual da API) para centavos.
 * `Math.round` absorve o erro de ponto flutuante (ex.: 0.1 + 0.2).
 *
 * @example decimalToCents(150.5) // 15050
 */
export function decimalToCents(value: number): Cents {
  if (!Number.isFinite(value)) {
    throw new TypeError(`decimalToCents espera um número finito, recebeu: ${value}`);
  }

  return Math.round(value * 100);
}

/**
 * Caminho inverso de `decimalToCents`: centavos → decimal em reais, para enviar à API.
 *
 * @example centsToDecimal(15050) // 150.5
 */
export function centsToDecimal(cents: Cents): DecimalAmount {
  if (!Number.isSafeInteger(cents)) {
    throw new TypeError(`centsToDecimal espera um inteiro em centavos, recebeu: ${cents}`);
  }

  return cents / 100;
}

const horaFormatter = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });
const diaFormatter = new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "long", year: "numeric" });

/** "2026-10-12T11:19:53Z" → "08:19" (no fuso do navegador). */
export function formatHora(dataHora: IsoDateTime): string {
  return horaFormatter.format(new Date(dataHora));
}

/** Data por extenso, com "Hoje" e "Ontem" para os dias mais recentes. Ex.: "12 de outubro de 2026". */
export function formatDia(data: Date, agora: Date = new Date()): string {
  const ontem = new Date(agora);
  ontem.setDate(agora.getDate() - 1);

  if (mesmoDia(data, agora)) return "Hoje";
  if (mesmoDia(data, ontem)) return "Ontem";
  return diaFormatter.format(data);
}

function mesmoDia(a: Date, b: Date): boolean {
  return a.toDateString() === b.toDateString();
}
