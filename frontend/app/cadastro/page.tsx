import type { Metadata } from "next";
import { CadastroScreen } from "@/components/cadastro/cadastro-screen";

export const metadata: Metadata = {
  title: "Abrir conta",
  description: "Abra sua conta no RKF Banco Digital em menos de 2 minutos.",
};

export default function CadastroPage() {
  return <CadastroScreen />;
}
