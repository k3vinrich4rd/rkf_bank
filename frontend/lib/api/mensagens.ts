/**
 * Aviso para quando a API está hibernando (plano gratuito do Render: dorme após 15 min sem uso e leva ~1 min para
 * acordar). Fica num arquivo próprio porque o BFF (servidor) e o `apiRequest` (navegador) mostram o mesmo texto, e o
 * navegador não deve carregar o código que fala com a API direto.
 */
export const MENSAGEM_SERVIDOR_ACORDANDO =
  "O servidor está acordando: no plano gratuito ele dorme quando fica sem uso e leva cerca de 1 minuto para voltar. " +
  "Tente de novo em instantes.";
