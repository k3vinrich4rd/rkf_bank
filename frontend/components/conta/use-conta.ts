import { useEffect, useState } from "react";
import { ApiError, contasApi, isFalhaDeServidor, type ContaResponse, type Uuid } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { useContaAtual } from "./use-conta-atual";

/**
 * O que a tela mostra:
 * - `carregando`: lendo o navegador ou buscando a conta na API;
 * - `sem-cadastro`: nenhum cliente guardado → mandar para o cadastro;
 * - `sem-conta`: cliente guardado, mas sem conta aberta → mandar para a abertura;
 * - `erro`: a busca falhou → mensagem e "tentar de novo";
 * - `pronta`: conta carregada.
 */
export type EtapaConta = "carregando" | "sem-cadastro" | "sem-conta" | "erro" | "pronta";

/**
 * Resultado de uma busca, marcado com a busca que o gerou. Uma busca nova (outra conta
 * ou "tentar de novo") muda a chave, e o resultado antigo deixa de valer sozinho —
 * sem precisar zerar estado dentro do efeito.
 */
type Resultado = { chave: string } & ({ conta: ContaResponse } | { erro: string });

function chaveDaBusca(contaId: Uuid, tentativa: number) {
  return `${contaId}#${tentativa}`;
}

function mensagemDeErro(error: unknown): string {
  return isFalhaDeServidor(error)
    ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
    : "Não conseguimos carregar sua conta. Tente novamente.";
}

/** Carrega a conta guardada no navegador (ver `lib/conta-atual.ts`). */
export function useConta() {
  const logTela = useLogDeTela("conta");
  const { conta: contaAtual, carregando: lendoNavegador, salvar } = useContaAtual();
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  const clienteId = contaAtual?.clienteId;
  const contaId = contaAtual?.contaId;

  useEffect(() => {
    if (!clienteId || !contaId) return;

    let ativa = true;
    const chave = chaveDaBusca(contaId, tentativa);

    contasApi.buscar(contaId).then(
      (conta) => {
        if (ativa) setResultado({ chave, conta });
      },
      (error: unknown) => {
        if (!ativa) return;
        if (error instanceof ApiError && error.status === 404) {
          // A conta guardada não existe mais (ex.: banco recriado): mantém o cliente e pede nova abertura.
          logTela.falha("Conta guardada não encontrada", error, { contaId });
          salvar({ clienteId });
          return;
        }
        const mensagem = mensagemDeErro(error);
        logTela.falha("Conta não carregada", error, { contaId, mensagemExibida: mensagem });
        setResultado({ chave, erro: mensagem });
      },
    );

    return () => {
      ativa = false;
    };
  }, [clienteId, contaId, tentativa, logTela, salvar]);

  // Só vale o resultado da busca atual; um resultado antigo conta como "ainda carregando".
  const atual = contaId && resultado?.chave === chaveDaBusca(contaId, tentativa) ? resultado : null;

  return {
    etapa: definirEtapa({ lendoNavegador, clienteId, contaId, atual }),
    conta: atual && "conta" in atual ? atual.conta : null,
    erro: atual && "erro" in atual ? atual.erro : null,
    tentarDeNovo: () => setTentativa((n) => n + 1),
  };
}

interface EstadoDaEtapa {
  lendoNavegador: boolean;
  clienteId: Uuid | undefined;
  contaId: Uuid | undefined;
  atual: Resultado | null;
}

function definirEtapa({ lendoNavegador, clienteId, contaId, atual }: EstadoDaEtapa): EtapaConta {
  if (lendoNavegador) return "carregando";
  if (!clienteId) return "sem-cadastro";
  if (!contaId) return "sem-conta";
  if (!atual) return "carregando";
  return "conta" in atual ? "pronta" : "erro";
}

export type ContaState = ReturnType<typeof useConta>;
