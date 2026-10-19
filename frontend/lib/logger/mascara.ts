/**
 * Mascaramento de dados pessoais (LGPD).
 *
 * O logger passa todo contexto por `mascararDadosPessoais` antes de escrever,
 * então quem loga não precisa lembrar de mascarar nada. Para proteger um campo
 * novo, basta adicioná-lo em `MASCARAS`.
 */

const OCULTO = "***";

const MASCARAS: Record<string, (valor: string) => string> = {
  // 52998224725 → ***.***.247-25
  cpf: (valor) => {
    const digitos = valor.replace(/\D/g, "");
    return digitos.length === 11 ? `***.***.${digitos.slice(6, 9)}-${digitos.slice(9)}` : OCULTO;
  },
  // joao.silva@rkfbank.com → j***@rkfbank.com
  email: (valor) => {
    const arroba = valor.indexOf("@");
    return arroba > 0 ? `${valor[0]}${OCULTO}${valor.slice(arroba)}` : OCULTO;
  },
  // 11999998888 → ***8888
  telefone: (valor) => `${OCULTO}${valor.replace(/\D/g, "").slice(-4)}`,
  // Joao Silva → Joao ***
  nomeCompleto: (valor) => `${valor.trim().split(/\s+/)[0] ?? ""} ${OCULTO}`,
  // Credenciais nunca aparecem, nem em parte.
  senha: () => OCULTO,
  confirmarSenha: () => OCULTO,
  token: () => OCULTO,
};

const PROFUNDIDADE_MAXIMA = 3;

export function mascararDadosPessoais(valor: unknown, profundidade = 0): unknown {
  if (profundidade > PROFUNDIDADE_MAXIMA || valor === null || typeof valor !== "object") return valor;
  if (Array.isArray(valor)) return valor.map((item) => mascararDadosPessoais(item, profundidade + 1));

  return Object.fromEntries(
    Object.entries(valor).map(([chave, item]) => {
      const mascara = MASCARAS[chave];
      if (mascara && typeof item === "string") return [chave, mascara(item)];
      return [chave, mascararDadosPessoais(item, profundidade + 1)];
    }),
  );
}
