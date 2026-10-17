import type { Metadata } from "next";
import { MovimentacaoScreen } from "@/components/movimentacao/movimentacao-screen";

export const metadata: Metadata = {
  title: "Depositar",
  robots: { index: false, follow: false },
};

export default function DepositarPage() {
  return <MovimentacaoScreen tipo="deposito" />;
}
