import type { Metadata } from "next";
import { TrocarSenhaScreen } from "@/components/trocar-senha/trocar-senha-screen";

export const metadata: Metadata = {
  title: "Trocar senha",
  robots: { index: false, follow: false },
};

export default function TrocarSenhaPage() {
  return <TrocarSenhaScreen />;
}
