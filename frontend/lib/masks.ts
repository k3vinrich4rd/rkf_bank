/**
 * Máscaras de exibição para campos de formulário.
 *
 * O usuário digita com formatação, mas a API recebe só dígitos —
 * use `onlyDigits` antes de enviar.
 */

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

/** 11999998888 → (11) 99999-8888 · 1133334444 → (11) 3333-4444 */
export function maskTelefone(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  const middleLength = digits.length === 11 ? 5 : 4;

  return digits
    .replace(/(\d{2})(\d)/, "($1) $2")
    .replace(new RegExp(`(\\d{${middleLength}})(\\d{1,4})$`), "$1-$2");
}
