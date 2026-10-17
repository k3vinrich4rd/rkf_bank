"use client";

import { useLogDeTela } from "./use-log-de-tela";

/**
 * Registra a abertura/fechamento de uma tela sem precisar de hook.
 * Não renderiza nada — pode ser colocado direto num `page.tsx` (Server Component):
 *
 *   export default function LoginPage() {
 *     return (
 *       <>
 *         <LogDeTela tela="login" />
 *         <LoginScreen />
 *       </>
 *     );
 *   }
 *
 * Se a tela também precisa logar ações do usuário, use `useLogDeTela` no lugar.
 */
export function LogDeTela({ tela }: { tela: string }) {
  useLogDeTela(tela);
  return null;
}
