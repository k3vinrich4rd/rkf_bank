/**
 * Máscaras de exibição para campos de formulário.
 *
 * O usuário digita com formatação, mas a API recebe só dígitos —
 * use `onlyDigits` antes de enviar.
 */

import { formatCurrency } from "@/lib/format";
import type { Cents } from "@/types";

export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

/** 52998224725 → 529.982.247-25 */
export function maskCpf(value: string): string {
  return onlyDigits(value)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

/** 01001000 → 01001-000 */
export function maskCep(value: string): string {
  return onlyDigits(value)
    .slice(0, 8)
    .replace(/(\d{5})(\d)/, "$1-$2");
}

/** Até R$ 999.999.999,99: bem acima de qualquer saldo realista, e longe do limite seguro do `number`. */
const MAX_DIGITOS_MOEDA = 11;

/**
 * Valor em reais digitado como em app de banco: os dígitos entram pelos centavos.
 * 5 → R$ 0,05 · 15050 → R$ 150,50. Campo vazio continua vazio.
 */
export function maskMoeda(value: string): string {
  const digits = onlyDigits(value).replace(/^0+/, "").slice(0, MAX_DIGITOS_MOEDA);
  return digits ? formatCurrency(Number(digits)) : "";
}

/** "R$ 150,50" → 15050 centavos. Campo vazio vale 0. */
export function centsFromMoeda(value: string): Cents {
  return Number(onlyDigits(value) || 0);
}

/** 11999998888 → (11) 99999-8888 · 1133334444 → (11) 3333-4444 */
export function maskTelefone(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  const middleLength = digits.length === 11 ? 5 : 4;

  return digits
    .replace(/(\d{2})(\d)/, "($1) $2")
    .replace(new RegExp(`(\\d{${middleLength}})(\\d{1,4})$`), "$1-$2");
}
