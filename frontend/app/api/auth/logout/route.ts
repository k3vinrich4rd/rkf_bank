import { cookies } from "next/headers";
import { COOKIE_REFRESH, apagarSessao } from "@/lib/auth/cookies";
import { revogarSessao } from "@/lib/auth/renovar";

/**
 * Sair: revoga o token de renovação na API (`POST /auth/logout`, contrato 3.1) e apaga os três cookies. O token de
 * acesso é stateless e ainda valeria até vencer, mas sem o cookie o `proxy.ts` deixa de enviá-lo.
 */
export async function POST() {
  const cookieStore = await cookies();
  await revogarSessao(cookieStore.get(COOKIE_REFRESH)?.value);
  apagarSessao(cookieStore);
  return new Response(null, { status: 204 });
}
