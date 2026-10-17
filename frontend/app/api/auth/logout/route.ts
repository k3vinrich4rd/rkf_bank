import { cookies } from "next/headers";
import { COOKIE_SESSAO, COOKIE_TOKEN } from "@/lib/auth/cookies";

/**
 * Sair. O JWT é stateless (o backend não guarda sessão), então sair é só apagar os cookies:
 * sem o cookie, o `proxy.ts` deixa de enviar o token.
 */
export async function POST() {
  const cookieStore = await cookies();
  cookieStore.delete(COOKIE_TOKEN);
  cookieStore.delete(COOKIE_SESSAO);
  return new Response(null, { status: 204 });
}
