import { useEffect, useState } from "react";
import { contasApi, isFalhaDeServidor, type LancamentoResponse, type Uuid } from "@/lib/api";
import { useConta } from "@/components/conta/use-conta";
import { useLogDeTela } from "@/components/logs";

/**
 * O que a lista mostra, depois que a conta já carregou (as etapas da conta ficam no `useConta`):
 * - `carregando`: buscando os lançamentos;
 * - `erro`: a busca falhou → mensagem e "tentar de novo";
 * - `vazio`: conta sem movimentação;
 * - `pronto`: lançamentos do mais recente para o mais antigo (ordem da API).
 */
export type EtapaExtrato = "carregando" | "erro" | "vazio" | "pronto";

/** Mesmo esquema do `useConta`: o resultado carrega a busca que o gerou, e um resultado antigo deixa de valer. */
type Resultado = { chave: string } & ({ lancamentos: LancamentoResponse[] } | { erro: string });

function chaveDaBusca(contaId: Uuid, tentativa: number) {
  return `${contaId}#${tentativa}`;
}

function mensagemDeErro(error: unknown): string {
  return isFalhaDeServidor(error)
    ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
    : "Não conseguimos carregar seu extrato. Tente novamente.";
}

/** Carrega a conta guardada no navegador e, com ela pronta, os lançamentos dela. */
export function useExtrato() {
  const logTela = useLogDeTela("extrato");
  const conta = useConta(logTela);
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  const contaId = conta.conta?.id;

  useEffect(() => {
    if (!contaId) return;

    let ativa = true;
    const chave = chaveDaBusca(contaId, tentativa);

    contasApi.extrato(contaId).then(
      (lancamentos) => {
        if (ativa) setResultado({ chave, lancamentos });
      },
      (error: unknown) => {
        if (!ativa) return;
        const mensagem = mensagemDeErro(error);
        logTela.falha("Extrato não carregado", error, { contaId, mensagemExibida: mensagem });
        setResultado({ chave, erro: mensagem });
      },
    );

    return () => {
      ativa = false;
    };
  }, [contaId, tentativa, logTela]);

  const atual = contaId && resultado?.chave === chaveDaBusca(contaId, tentativa) ? resultado : null;
  const lancamentos = atual && "lancamentos" in atual ? atual.lancamentos : [];

  return {
    conta,
    etapa: definirEtapa(atual),
    lancamentos,
    erro: atual && "erro" in atual ? atual.erro : null,
    tentarDeNovo: () => setTentativa((n) => n + 1),
  };
}

function definirEtapa(atual: Resultado | null): EtapaExtrato {
  if (!atual) return "carregando";
  if ("erro" in atual) return "erro";
  return atual.lancamentos.length > 0 ? "pronto" : "vazio";
}

export type ExtratoState = ReturnType<typeof useExtrato>;
