import type { Metadata } from "next";
import { ContaScreen } from "@/components/conta/conta-screen";

export const metadata: Metadata = {
  title: "Sua conta",
  robots: { index: false, follow: false },
};

export default function ContaPage() {
  return <ContaScreen />;
}
