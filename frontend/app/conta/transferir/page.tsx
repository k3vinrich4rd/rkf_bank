import type { Metadata } from "next";
import { MovimentacaoScreen } from "@/components/movimentacao/movimentacao-screen";

export const metadata: Metadata = {
  title: "Transferir",
  robots: { index: false, follow: false },
};

export default function TransferirPage() {
  return <MovimentacaoScreen tipo="transferencia" />;
}
