import type { Metadata } from "next";
import { TrocarSenhaScreen } from "@/components/trocar-senha/trocar-senha-screen";

export const metadata: Metadata = {
  title: "Trocar senha",
  robots: { index: false, follow: false },
};

/** `?temporaria=1`: a senha foi redefinida pelo ADMIN e a API só libera a troca (contrato 3.1). */
export default async function TrocarSenhaPage({ searchParams }: PageProps<"/conta/senha">) {
  const { temporaria } = await searchParams;
  return <TrocarSenhaScreen temporaria={temporaria === "1"} />;
}
