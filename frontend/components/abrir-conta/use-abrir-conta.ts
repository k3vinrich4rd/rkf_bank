import { useState, type FormEvent } from "react";
import { ApiError, contasApi, isFalhaDeServidor, type ContaResponse, type TipoConta, type Uuid } from "@/lib/api";
import { useContaAtual } from "@/components/conta/use-conta-atual";
import { useLogDeTela } from "@/components/logs";
import type { ContaAtual } from "@/lib/conta-atual";

/**
 * O que a tela mostra:
 * - `carregando`: ainda lendo o navegador (evita piscar "sem cadastro");
 * - `sem-cadastro`: nenhum cliente guardado → mandar para o cadastro;
 * - `ja-tem-conta`: o navegador já guarda uma conta deste cliente;
 * - `formulario`: escolher o tipo e abrir;
 * - `sucesso`: conta aberta agora.
 */
export type EtapaAbrirConta = "carregando" | "sem-cadastro" | "ja-tem-conta" | "formulario" | "sucesso";

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

/** Traduz qualquer falha da abertura numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
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
 * O cliente vem do navegador (gravado pelo cadastro), já que ainda não existe login.
 */
export function useAbrirConta() {
  const logTela = useLogDeTela("abrir-conta");
  const { conta: contaAtual, carregando, salvar } = useContaAtual();
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
    if (!contaAtual) return;

    setEnviando(true);
    setErroGeral(null);
    logTela.acao("Abertura de conta enviada", { tipoConta });

    try {
      const conta = await abrirComNumeroLivre(contaAtual.clienteId, tipoConta);
      logTela.sucesso("Conta aberta", { contaId: conta.id });
      // A tela de sucesso é decidida por `contaAberta`; salvar a conta não troca a etapa.
      setContaAberta(conta);
      salvar({ clienteId: conta.clienteId, contaId: conta.id });
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        // O cliente guardado não existe mais no backend (ex.: banco recriado): esquece e pede novo cadastro.
        logTela.falha("Cliente guardado não encontrado", error);
        salvar(null);
        return;
      }

      const mensagem = mensagemDeErro(error);
      logTela.falha("Abertura de conta não concluída", error, { mensagemExibida: mensagem });
      setErroGeral(mensagem);
    } finally {
      setEnviando(false);
    }
  }

  return {
    etapa: definirEtapa({ contaAberta, carregando, contaAtual }),
    tipoConta,
    escolherTipo,
    enviando,
    erroGeral,
    contaAberta,
    handleSubmit,
  };
}

interface EstadoDaEtapa {
  contaAberta: ContaResponse | null;
  carregando: boolean;
  contaAtual: ContaAtual | null;
}

function definirEtapa({ contaAberta, carregando, contaAtual }: EstadoDaEtapa): EtapaAbrirConta {
  if (contaAberta) return "sucesso";
  if (carregando) return "carregando";
  if (!contaAtual) return "sem-cadastro";
  if (contaAtual.contaId) return "ja-tem-conta";
  return "formulario";
}

export type AbrirContaState = ReturnType<typeof useAbrirConta>;
