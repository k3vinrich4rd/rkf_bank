import type { Ref } from "react";
import { APP, APP_LANCAMENTOS, SALDO_FINAL, TITULAR, formatarLancamento, formatarReais } from "./conteudo";

interface CelularAppProps {
  ref: Ref<HTMLDivElement>;
  vagaRef: Ref<HTMLDivElement>;
}

/**
 * Celular desenhado em CSS com a tela da conta, no mesmo desenho da `/conta` de verdade.
 * O cartão 3D não está aqui: ele encaixa por cima da `.app__vaga`, cujo retângulo o hero mede.
 * Os itens `.app-item` aparecem só quando o cartão encaixa.
 */
export function CelularApp({ ref, vagaRef }: CelularAppProps) {
  const primeiroNome = TITULAR.split(" ")[0];

  return (
    <div ref={ref} className="celular" aria-hidden="true">
      <div className="celular__tela">
        <span className="celular__ilha" />

        <div className="app-item flex items-center justify-between text-[0.85em] text-[var(--l-suave)]">
          <span>Olá, {primeiroNome}</span>
          <span className="font-bold tracking-[0.2em] text-[var(--l-texto)]">RKF</span>
        </div>
        <p className="app-item mt-[0.4em] text-[1.45em] font-semibold tracking-tight">Sua conta</p>
        <p className="app-item fonte-mono mb-[1em] text-[0.72em] text-[var(--l-suave)]">{APP.conta}</p>

        <div ref={vagaRef} className="app__vaga" />

        <div className="app-item mt-[1.3em]">
          <p className="text-[0.8em] text-[var(--l-suave)]">Saldo disponível</p>
          <p className="fonte-mono mt-[0.15em] text-[1.6em] font-medium tracking-tight">{formatarReais(SALDO_FINAL)}</p>
        </div>
        <div className="app-item mt-[0.8em] h-px w-full overflow-hidden bg-[var(--l-fio)]">
          <div className="h-full w-[62%] bg-gradient-to-r from-[var(--l-azul)] to-[var(--l-acento)]" />
        </div>

        <div className="app-item mt-[1em] grid grid-cols-4 gap-[0.4em] text-center text-[0.72em]">
          {APP.acoes.map((acao) => (
            <span key={acao} className="rounded-[0.7em] border border-[var(--l-fio)] py-[0.7em]">
              {acao}
            </span>
          ))}
        </div>

        <ul className="mt-[1.1em] flex flex-col">
          {APP_LANCAMENTOS.map((lancamento) => (
            <li
              key={lancamento.hora}
              className="app-item flex items-center justify-between border-t border-[var(--l-fio)] py-[0.7em] text-[0.8em]"
            >
              <span>
                <span className="block">{lancamento.descricao}</span>
                <span className="fonte-mono text-[0.85em] text-[var(--l-suave)]">Hoje, {lancamento.hora}</span>
              </span>
              <span className={`fonte-mono ${lancamento.valor > 0 ? "text-emerald-400" : ""}`}>
                {formatarLancamento(lancamento.valor)}
              </span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
