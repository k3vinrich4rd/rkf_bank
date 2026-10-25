import { CartaoEmVitrine } from "@/components/auth/cartao-em-vitrine";
import { Vitrine } from "@/components/auth/vitrine";

/** O que a conta entrega hoje (contrato da API): nada de promessa que o banco ainda não cumpre. */
const FATOS = [
  "Conta corrente ou poupança, sem mensalidade",
  "Depósito, saque e transferência na hora",
  "Extrato agrupado por dia, centavo por centavo",
] as const;

interface CadastroShowcaseProps {
  /** Nome digitado no formulário: aparece no cartão em tempo real. */
  nome: string;
}

/** Vitrine do cadastro (desktop): o cartão ganha o nome da pessoa enquanto ela digita. */
export function CadastroShowcase({ nome }: CadastroShowcaseProps) {
  return (
    <Vitrine
      rotulo="Conta digital gratuita"
      destaque={
        <CartaoEmVitrine
          titular={nome || "Seu nome aqui"}
          legenda={nome ? "Esse é o seu cartão RKF." : "Digite seu nome e veja seu cartão ganhar vida."}
        />
      }
      titulo={
        <>
          Seu dinheiro, <span className="text-gradient-brand">do seu jeito.</span>
        </>
      }
      fatos={FATOS}
    />
  );
}
