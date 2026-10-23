import { useCallback, useEffect, useState } from "react";
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
type Resultado = { tentativa: number } & ({ contas: ContaResponse[] } | { erro: string });

/**
 * Conta escolhida no seletor, lembrada neste navegador para valer em todas as telas (saldo, extrato,
 * movimentações). Guarda só o id da conta, que não identifica ninguém sem o token.
 */
const CHAVE_CONTA_ESCOLHIDA = "rkf_conta_escolhida";

function lerContaEscolhida(): string | null {
  try {
    return window.localStorage.getItem(CHAVE_CONTA_ESCOLHIDA);
  } catch {
    // Navegação privada ou armazenamento bloqueado: fica na primeira conta.
    return null;
  }
}

function guardarContaEscolhida(contaId: string) {
  try {
    window.localStorage.setItem(CHAVE_CONTA_ESCOLHIDA, contaId);
  } catch {
    // Sem armazenamento a escolha vale só nesta tela.
  }
}

function mensagemDeErro(error: unknown): string {
  return isFalhaDeServidor(error)
    ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
    : "Não conseguimos carregar sua conta. Tente novamente.";
}

/**
 * Carrega a conta do usuário logado. Usado por todas as telas da conta;
 * `logTela` é o log da tela que chamou, para as falhas aparecerem no nome dela.
 *
 * Um cliente pode ter mais de uma conta (contrato 4.1): `contas` traz todas as do próprio cliente
 * (`contasApi.minhas` filtra pela sessão, o que importa para o ADMIN) e `conta` é a escolhida no seletor,
 * ou a primeira enquanto ninguém escolheu.
 */
export function useConta(logTela: TelaLogger) {
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [escolhida, setEscolhida] = useState<string | null>(null);


  useEffect(() => {
    let ativa = true;

    contasApi.minhas().then(
      (contas) => {
        if (!ativa) return;
        // Lida aqui, com as contas já no navegador: no servidor não há localStorage (ler no render quebraria a hidratação).
        setEscolhida(lerContaEscolhida());
        setResultado({ tentativa, contas });
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
  const contas = atual && "contas" in atual ? atual.contas : [];
  // Conta escolhida que sumiu (encerrada, outra pessoa no mesmo navegador) volta para a primeira.
  const conta = contas.find((c) => c.id === escolhida) ?? contas[0] ?? null;

  const escolherConta = useCallback((contaId: string) => {
    guardarContaEscolhida(contaId);
    setEscolhida(contaId);
  }, []);

  return {
    etapa: definirEtapa(atual),
    conta,
    contas,
    /** Troca a conta das telas da conta; a escolha fica lembrada neste navegador. */
    escolherConta,
    erro: atual && "erro" in atual ? atual.erro : null,
    /** Busca de novo: depois de um erro ou de uma movimentação que mudou o saldo. */
    tentarDeNovo: () => setTentativa((n) => n + 1),
  };
}

function definirEtapa(atual: Resultado | null): EtapaConta {
  if (!atual) return "carregando";
  if ("erro" in atual) return "erro";
  return atual.contas.length > 0 ? "pronta" : "sem-conta";
}

export type ContaState = ReturnType<typeof useConta>;
