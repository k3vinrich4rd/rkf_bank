import { fonteLarga, fonteMono } from "@/components/landing/fontes";
import { HeroCartao } from "@/components/landing/hero-cartao";
import { LandingMovimento } from "@/components/landing/landing-movimento";
import { LandingNav } from "@/components/landing/landing-nav";
import { LandingRodape } from "@/components/landing/landing-rodape";
import { SecaoCep } from "@/components/landing/secao-cep";
import { SecaoContas } from "@/components/landing/secao-contas";
import { SecaoExtrato } from "@/components/landing/secao-extrato";
import { SecaoTime } from "@/components/landing/secao-time";
import { SecaoUmaVez } from "@/components/landing/secao-uma-vez";
import "@/components/landing/landing.css";

/**
 * Landing page. O HTML sai pronto do servidor (SEO e primeira pintura); o movimento (cartão 3D, pins e
 * rolagem suave) liga no navegador, dentro de cada seção.
 */
export default function HomePage() {
  return (
    <div className={`landing ${fonteLarga.variable} ${fonteMono.variable} flex-1`}>
      <LandingMovimento>
        <LandingNav />
        <main>
          <HeroCartao />
          <SecaoExtrato />
          <SecaoUmaVez />
          <SecaoCep />
          <SecaoTime />
          <SecaoContas />
        </main>
        <LandingRodape />
      </LandingMovimento>
    </div>
  );
}
