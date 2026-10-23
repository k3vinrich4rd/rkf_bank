import type { Metadata } from "next";
import { MeusDadosScreen } from "@/components/meus-dados/meus-dados-screen";

export const metadata: Metadata = {
  title: "Meus dados",
  robots: { index: false, follow: false },
};

export default function MeusDadosPage() {
  return <MeusDadosScreen />;
}
