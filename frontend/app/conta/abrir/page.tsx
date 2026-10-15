import type { Metadata } from "next";
import { AbrirContaScreen } from "@/components/abrir-conta/abrir-conta-screen";

export const metadata: Metadata = {
  title: "Escolha sua conta",
  robots: { index: false, follow: false },
};

export default function AbrirContaPage() {
  return <AbrirContaScreen />;
}
