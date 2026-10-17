import type { Metadata } from "next";
import { MovimentacaoScreen } from "@/components/movimentacao/movimentacao-screen";

export const metadata: Metadata = {
  title: "Sacar",
  robots: { index: false, follow: false },
};

export default function SacarPage() {
  return <MovimentacaoScreen tipo="saque" />;
}
