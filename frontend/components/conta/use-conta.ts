import { useEffect, useState } from "react";
import { contasApi, isFalhaDeServidor, type ContaResponse } from "@/lib/api";
import type { TelaLogger } from "@/components/logs";

/**
 * O que a tela mostra:
 * - `carregando`: buscando as contas do usuário logado;
 * - `sem-conta`: logado, mas sem conta aberta → mandar para a abertura;
 * - `erro`: a busca falhou → mensagem e "tentar de novo";
 * - `pronta`: conta carregada.
 *
 * Sem login não se chega aqui: o `proxy.ts` manda para o login, e um token vencido no meio do caminho
 * vira 401, que o `apiRequest` também manda para o login.
 */
export type EtapaConta = "carregando" | "sem-conta" | "erro" | "pronta";

/**
 * Resultado de uma busca, marcado com a tentativa que o gerou. "Tentar de novo" muda a tentativa, e o
 * resultado antigo deixa de valer sozinho — sem precisar zerar estado dentro do efeito.
 */
type Resultado = { tentativa: number } & ({ conta: ContaResponse | null } | { erro: string });

function mensagemDeErro(error: unknown): string {
  return isFalhaDeServidor(error)
    ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
    : "Não conseguimos carregar sua conta. Tente novamente.";
}

/**
 * Carrega a conta do usuário logado. Usado por todas as telas da conta;
 * `logTela` é o log da tela que chamou, para as falhas aparecerem no nome dela.
 *
 * Um cliente pode ter mais de uma conta (contrato 4.1), mas a interface ainda trabalha com uma só:
 * a primeira que a API devolve.
 */
export function useConta(logTela: TelaLogger) {
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  useEffect(() => {
    let ativa = true;

    contasApi.minhas().then(
      ([primeira]) => {
        if (ativa) setResultado({ tentativa, conta: primeira ?? null });
      },
      (error: unknown) => {
        if (!ativa) return;
        const mensagem = mensagemDeErro(error);
        logTela.falha("Conta não carregada", error, { mensagemExibida: mensagem });
        setResultado({ tentativa, erro: mensagem });
      },
    );

    return () => {
      ativa = false;
    };
  }, [tentativa, logTela]);

  // Só vale o resultado da busca atual; um resultado antigo conta como "ainda carregando".
  const atual = resultado?.tentativa === tentativa ? resultado : null;
  const conta = atual && "conta" in atual ? atual.conta : null;

  return {
    etapa: definirEtapa(atual),
    conta,
    erro: atual && "erro" in atual ? atual.erro : null,
    /** Busca de novo: depois de um erro ou de uma movimentação que mudou o saldo. */
    tentarDeNovo: () => setTentativa((n) => n + 1),
  };
}

function definirEtapa(atual: Resultado | null): EtapaConta {
  if (!atual) return "carregando";
  if ("erro" in atual) return "erro";
  return atual.conta ? "pronta" : "sem-conta";
}

export type ContaState = ReturnType<typeof useConta>;
