import type { Metadata } from "next";
import { LoginScreen } from "@/components/login/login-screen";
import { destinoDepoisDoLogin } from "@/lib/routes";

export const metadata: Metadata = {
  title: "Entrar",
  robots: { index: false, follow: false },
};

/**
 * `?voltar=` vem do `proxy.ts` (tela protegida aberta sem sessão) ou do `apiRequest` (401 no meio do uso),
 * que também manda `?expirou=1`.
 */
export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { voltar, expirou } = await searchParams;

  return (
    <LoginScreen
      destino={destinoDepoisDoLogin(typeof voltar === "string" ? voltar : undefined)}
      expirou={expirou === "1"}
    />
  );
}
