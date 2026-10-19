import { useState, type FormEvent } from "react";
import { ApiError, authApi, contasApi, isFalhaDeServidor, type ContaResponse, type TipoConta, type Uuid } from "@/lib/api";
import { useConta, type ContaState } from "@/components/conta/use-conta";
import { useLogDeTela } from "@/components/logs";

/**
 * O que a tela mostra:
 * - `carregando`: conferindo se o usuário logado já tem conta;
 * - `erro-conta`: essa conferência falhou → mensagem e "tentar de novo";
 * - `ja-tem-conta`: o usuário logado já tem conta;
 * - `formulario`: escolher o tipo e abrir;
 * - `sucesso`: conta aberta agora.
 */
export type EtapaAbrirConta = "carregando" | "erro-conta" | "ja-tem-conta" | "formulario" | "sucesso";

// TODO(backend): apagar a geração de agência/número quando o backend passar a gerá-los
// (ver planing/debitos-tecnicos.md). Até lá, o número é sorteado aqui e, se já existir (409),
// sorteamos de novo.
const AGENCIA_PADRAO = "0001";
const TENTATIVAS_NUMERO_LIVRE = 3;

/** Número de 8 dígitos, sem zero à esquerda (o contrato aceita de 5 a 12). */
function sortearNumeroConta(): string {
  const [aleatorio] = crypto.getRandomValues(new Uint32Array(1));
  return String(10_000_000 + (aleatorio % 90_000_000));
}

async function abrirComNumeroLivre(clienteId: Uuid, tipoContaEnum: TipoConta): Promise<ContaResponse> {
  for (let tentativa = 1; ; tentativa++) {
    try {
      return await contasApi.abrir(clienteId, {
        agencia: AGENCIA_PADRAO,
        numeroConta: sortearNumeroConta(),
        tipoContaEnum,
      });
    } catch (error) {
      const numeroJaExiste = error instanceof ApiError && error.status === 409;
      if (!numeroJaExiste || tentativa >= TENTATIVAS_NUMERO_LIVRE) throw error;
    }
  }
}

/** Perfil sem cliente (o ADMIN): não tem cadastro em que abrir conta. */
class SemCadastroDeCliente extends Error {}

/** Traduz qualquer falha da abertura numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
  if (error instanceof SemCadastroDeCliente) {
    return "Seu usuário não tem cadastro de cliente, então não pode abrir conta.";
  }
  if (isFalhaDeServidor(error)) {
    return "Não foi possível conectar ao servidor. Tente novamente em instantes.";
  }

  // Número de conta repetido em todas as tentativas.
  if (error instanceof ApiError && error.status === 409) {
    return "Não conseguimos gerar o número da sua conta. Tente novamente.";
  }

  return "Algo deu errado ao abrir sua conta. Tente novamente.";
}

/**
 * Toda a lógica da abertura de conta: quem é o cliente, tipo escolhido, envio e erros.
 * O cliente é o usuário logado: o `clienteId` vem da sessão (resposta do login).
 */
export function useAbrirConta() {
  const logTela = useLogDeTela("abrir-conta");
  const contaExistente = useConta(logTela);
  const [tipoConta, setTipoConta] = useState<TipoConta>("CORRENTE");
  const [enviando, setEnviando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [contaAberta, setContaAberta] = useState<ContaResponse | null>(null);

  function escolherTipo(tipo: TipoConta) {
    setTipoConta(tipo);
    setErroGeral(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEnviando(true);
    setErroGeral(null);
    logTela.acao("Abertura de conta enviada", { tipoConta });

    try {
      const { clienteId } = await authApi.sessao();
      if (!clienteId) throw new SemCadastroDeCliente();
      const conta = await abrirComNumeroLivre(clienteId, tipoConta);
      logTela.sucesso("Conta aberta", { contaId: conta.id });
      setContaAberta(conta);
    } catch (error) {
      const mensagem = mensagemDeErro(error);
      logTela.falha("Abertura de conta não concluída", error, { mensagemExibida: mensagem });
      setErroGeral(mensagem);
    } finally {
      setEnviando(false);
    }
  }

  return {
    etapa: definirEtapa(contaAberta, contaExistente),
    erroConta: contaExistente.erro,
    tentarDeNovo: contaExistente.tentarDeNovo,
    tipoConta,
    escolherTipo,
    enviando,
    erroGeral,
    contaAberta,
    handleSubmit,
  };
}

function definirEtapa(contaAberta: ContaResponse | null, contaExistente: ContaState): EtapaAbrirConta {
  if (contaAberta) return "sucesso";
  switch (contaExistente.etapa) {
    case "carregando":
      return "carregando";
    case "erro":
      return "erro-conta";
    case "pronta":
      return "ja-tem-conta";
    case "sem-conta":
      return "formulario";
  }
}

export type AbrirContaState = ReturnType<typeof useAbrirConta>;
