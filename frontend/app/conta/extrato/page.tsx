import type { Metadata } from "next";
import { ExtratoScreen } from "@/components/extrato/extrato-screen";

export const metadata: Metadata = {
  title: "Extrato",
  robots: { index: false, follow: false },
};

export default function ExtratoPage() {
  return <ExtratoScreen />;
}
