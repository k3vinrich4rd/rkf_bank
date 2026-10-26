import type { ReactNode } from "react";
import Link from "next/link";
import { ROUTES } from "@/lib/routes";
import { TERMOS_DATA_EXTENSO, TERMOS_VERSAO } from "@/lib/legal/termos";

interface DocumentoLegalProps {
  titulo: string;
  resumo: string;
  children: ReactNode;
}

/**
 * Moldura comum dos Termos de Uso e da Política de Privacidade: título, versão (a mesma que o cadastro manda em
 * `termosVersao`) e o aviso de que o texto é um rascunho até o time revisar.
 */
export function DocumentoLegal({ titulo, resumo, children }: DocumentoLegalProps) {
  return (
    <main className="mx-auto w-full max-w-3xl px-6 py-16 text-foreground">
      <Link href={ROUTES.home} className="font-mono text-xs tracking-[0.2em] text-brand-cyan uppercase">
        RKF Banco Digital
      </Link>
      <h1 className="mt-4 text-4xl font-semibold tracking-tight">{titulo}</h1>
      <p className="mt-3 text-muted-foreground">{resumo}</p>
      <p className="mt-2 text-sm text-muted-foreground">
        Versão <span className="font-mono">{TERMOS_VERSAO}</span> · publicada em {TERMOS_DATA_EXTENSO}
      </p>
      <p role="note" className="mt-6 rounded-lg border border-border px-4 py-3 text-sm text-muted-foreground">
        O RKF Bank é um projeto de estudo: não movimenta dinheiro de verdade. Use dados fictícios sempre que puder.
        Encarregados pelos dados pessoais: Kevin Richard e Renan Barros (seção 7 da Política de Privacidade).
      </p>
      <div className="mt-10 flex flex-col gap-8 leading-relaxed [&_h2]:text-xl [&_h2]:font-semibold [&_li]:ml-5 [&_li]:list-disc [&_section]:flex [&_section]:flex-col [&_section]:gap-3">
        {children}
      </div>
    </main>
  );
}
