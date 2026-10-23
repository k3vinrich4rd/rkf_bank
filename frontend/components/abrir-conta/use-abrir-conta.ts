import { useState, type FormEvent } from "react";
import { ApiError, contasApi, isFalhaDeServidor, type ContaResponse, type TipoConta } from "@/lib/api";
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

/** Traduz qualquer falha da abertura numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
  if (isFalhaDeServidor(error)) {
    return "Não foi possível conectar ao servidor. Tente novamente em instantes.";
  }

  // Perfil sem cliente (o ADMIN do `.env`): a API responde 422, não há cadastro em que abrir conta.
  if (error instanceof ApiError && error.status === 422) {
    return "Seu usuário não tem cadastro de cliente, então não pode abrir conta.";
  }

  // A API gera agência e número; 409 aqui só se ela não achar número livre.
  if (error instanceof ApiError && error.status === 409) {
    return "Não conseguimos gerar o número da sua conta. Tente novamente.";
  }

  return "Algo deu errado ao abrir sua conta. Tente novamente.";
}

/**
 * Toda a lógica da abertura de conta: quem é o cliente, tipo escolhido, envio e erros.
 * O cliente é o usuário logado: a API o tira do token (`POST /contas`, contrato 2.6).
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
      // Sem agência e número: a API usa a 0001 e gera o número com dígito verificador.
      const conta = await contasApi.abrir({ tipoConta });
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
