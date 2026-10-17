import { useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError, contasApi, isFalhaDeServidor, type ContaResponse, type Uuid } from "@/lib/api";
import { useConta } from "@/components/conta/use-conta";
import { useLogDeTela } from "@/components/logs";
import { centsToDecimal, decimalToCents, formatCurrency } from "@/lib/format";
import { centsFromMoeda, maskMoeda } from "@/lib/masks";
import type { Cents } from "@/types";
import { MOVIMENTACOES, type TipoMovimentacao } from "./movimentacoes";

export type Campo = "valor" | "descricao" | "contaDestinoId";
type Valores = Record<Campo, string>;
type ErrosPorCampo = Partial<Record<Campo, string>>;

/** O que a tela de sucesso mostra: o que foi pedido, já que a API responde `204` sem corpo. */
export interface Comprovante {
  valor: Cents;
  descricao?: string;
  contaDestinoId?: Uuid;
}

const VALORES_INICIAIS: Valores = { valor: "", descricao: "", contaDestinoId: "" };

const FORMATO_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Confere no navegador o que dá para saber sem a API, para o usuário não esperar uma ida ao servidor
 * só para descobrir que esqueceu o valor. O backend valida tudo de novo (regras R6 a R10 do contrato).
 */
function validar(tipo: TipoMovimentacao, valores: Valores, conta: ContaResponse): ErrosPorCampo {
  const erros: ErrosPorCampo = {};
  const valor = centsFromMoeda(valores.valor);
  const saldo = decimalToCents(conta.saldo);

  if (valor <= 0) {
    erros.valor = "Informe um valor maior que zero.";
  } else if (MOVIMENTACOES[tipo].debita && valor > saldo) {
    erros.valor = `Saldo insuficiente. Você tem ${formatCurrency(saldo)} disponível.`;
  }

  if (tipo === "transferencia") {
    const destino = valores.contaDestinoId.trim();
    if (!destino) erros.contaDestinoId = "Informe o ID da conta que vai receber.";
    else if (!FORMATO_UUID.test(destino)) erros.contaDestinoId = "ID inválido. Cole o ID completo da conta.";
    else if (destino.toLowerCase() === conta.id.toLowerCase()) erros.contaDestinoId = "Esse é o ID da sua própria conta.";
  }

  return erros;
}

async function executar(tipo: TipoMovimentacao, contaId: Uuid, comprovante: Comprovante) {
  const movimentacao = { valor: centsToDecimal(comprovante.valor), descricao: comprovante.descricao };

  switch (tipo) {
    case "deposito":
      return contasApi.depositar(contaId, movimentacao);
    case "saque":
      return contasApi.sacar(contaId, movimentacao);
    case "transferencia":
      return contasApi.transferir({
        ...movimentacao,
        contaOrigemId: contaId,
        contaDestinoId: comprovante.contaDestinoId ?? "",
      });
  }
}

/** Traduz qualquer falha do envio numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
  if (!(error instanceof ApiError) || isFalhaDeServidor(error)) {
    return "Não foi possível conectar ao servidor. Tente novamente em instantes.";
  }

  switch (error.status) {
    case 400:
    case 404: // na transferência, a conta de destino não existe (vira erro no campo)
      return "Confira os campos destacados.";
    case 409: // lock otimista: outra operação mexeu na conta ao mesmo tempo
      return "Sua conta acabou de ser movimentada por outra operação. Confira o saldo e tente novamente.";
    case 422: // conta inativa, saldo insuficiente ou mesma conta
      return error.message;
    default:
      return "Algo deu errado. Tente novamente.";
  }
}

function errosDaApi(tipo: TipoMovimentacao, error: unknown): ErrosPorCampo {
  if (!(error instanceof ApiError)) return {};
  if (error.status === 404 && tipo === "transferencia") {
    return { contaDestinoId: "Não encontramos uma conta com esse ID." };
  }
  return error.fieldErrors;
}

/**
 * Toda a lógica de depósito, saque e transferência: conta atual, campos, validação, envio e erros.
 * As três usam o mesmo contrato (`valor` + `descricao`); a transferência só acrescenta o destino.
 */
export function useMovimentacao(tipo: TipoMovimentacao) {
  const logTela = useLogDeTela(tipo);
  const conta = useConta(logTela);
  const [valores, setValores] = useState<Valores>(VALORES_INICIAIS);
  const [erros, setErros] = useState<ErrosPorCampo>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [comprovante, setComprovante] = useState<Comprovante | null>(null);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const campo = event.target.name as Campo;
    const valor = campo === "valor" ? maskMoeda(event.target.value) : event.target.value;

    setValores((atual) => ({ ...atual, [campo]: valor }));
    setErros((atual) => ({ ...atual, [campo]: undefined }));
    setErroGeral(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!conta.conta) return;

    const errosLocais = validar(tipo, valores, conta.conta);
    if (Object.keys(errosLocais).length > 0) {
      setErros(errosLocais);
      return;
    }

    const pedido: Comprovante = {
      valor: centsFromMoeda(valores.valor),
      descricao: valores.descricao.trim() || undefined,
      contaDestinoId: tipo === "transferencia" ? valores.contaDestinoId.trim() : undefined,
    };

    setEnviando(true);
    setErros({});
    setErroGeral(null);
    logTela.acao("Movimentação enviada", { tipo, contaId: conta.conta.id });

    try {
      await executar(tipo, conta.conta.id, pedido);
      logTela.sucesso("Movimentação concluída", { tipo, contaId: conta.conta.id });
      setComprovante(pedido);
      setValores(VALORES_INICIAIS);
      // A API responde 204 sem corpo: busca a conta de novo para mostrar o saldo atualizado.
      conta.tentarDeNovo();
    } catch (error) {
      const mensagem = mensagemDeErro(error);
      const errosCampos = errosDaApi(tipo, error);
      logTela.falha("Movimentação não concluída", error, {
        tipo,
        mensagemExibida: mensagem,
        ...(Object.keys(errosCampos).length > 0 && { camposComErro: Object.keys(errosCampos) }),
      });
      setErros(errosCampos);
      setErroGeral(mensagem);
    } finally {
      setEnviando(false);
    }
  }

  /** Props comuns a todo campo: nome, valor, erro e handler. */
  function campo(name: Campo) {
    return {
      name,
      value: valores[name],
      error: erros[name],
      onChange: handleChange,
      disabled: enviando,
    };
  }

  return {
    tipo,
    conta,
    campo,
    erroGeral,
    enviando,
    comprovante,
    handleSubmit,
    /** Volta ao formulário vazio depois de um sucesso. */
    novaMovimentacao: () => setComprovante(null),
  };
}

export type MovimentacaoState = ReturnType<typeof useMovimentacao>;
