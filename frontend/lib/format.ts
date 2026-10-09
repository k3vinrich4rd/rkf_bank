import type { Cents } from "@/types";

const brlFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/**
 * Formata um valor monetário em BRL.
 *
 * O backend trafega dinheiro em centavos (inteiro) para evitar erro de
 * ponto flutuante; a conversão para reais acontece só na borda de exibição.
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
