import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError, clientesApi, isFalhaDeServidor, type AtualizarClienteRequest, type ClienteResponse } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { maskCep, maskTelefone, onlyDigits } from "@/lib/masks";

/**
 * O que a tela mostra:
 * - `carregando`: buscando o cadastro do usuário logado;
 * - `erro`: a busca falhou → mensagem e "tentar de novo";
 * - `pronta`: cadastro carregado, formulário de correção liberado.
 */
export type EtapaMeusDados = "carregando" | "erro" | "pronta";

export type Campo = "nomeCompleto" | "email" | "telefone" | "cep" | "numero" | "complemento";
type Valores = Record<Campo, string>;
type ErrosPorCampo = Partial<Record<Campo, string>>;

const MASCARAS: Partial<Record<Campo, (valor: string) => string>> = { telefone: maskTelefone, cep: maskCep };

/** Resultado de uma busca, marcado com a tentativa que o gerou (mesmo padrão do `useConta`). */
type Resultado = { tentativa: number } & ({ cliente: ClienteResponse } | { erro: string });

function valoresDe(cliente: ClienteResponse): Valores {
  return {
    nomeCompleto: cliente.nomeCompleto,
    email: cliente.email,
    telefone: maskTelefone(cliente.telefone),
    cep: maskCep(cliente.endereco.cep),
    numero: cliente.endereco.numero,
    complemento: cliente.endereco.complemento ?? "",
  };
}

/**
 * Só o que mudou vai no `PATCH` (contrato 2.5): corpo vazio é `400` e o backend registra quais campos mudaram.
 * CEP novo exige o número, então ele vai junto mesmo que não tenha mudado.
 */
function mudancas(cliente: ClienteResponse, valores: Valores): AtualizarClienteRequest {
  const original = valoresDe(cliente);
  const pedido: AtualizarClienteRequest = {};
  const nome = valores.nomeCompleto.trim();
  const email = valores.email.trim();
  const telefone = onlyDigits(valores.telefone);
  const cep = onlyDigits(valores.cep);
  const numero = valores.numero.trim();
  const complemento = valores.complemento.trim();

  if (nome !== original.nomeCompleto) pedido.nomeCompleto = nome;
  if (email.toLowerCase() !== original.email.toLowerCase()) pedido.email = email;
  if (telefone !== onlyDigits(original.telefone)) pedido.telefone = telefone;
  if (cep !== onlyDigits(original.cep)) {
    pedido.cep = cep;
    pedido.numero = numero;
  }
  if (numero !== original.numero) pedido.numero = numero;
  // `""` apaga o complemento (contrato 2.5).
  if (complemento !== original.complemento) pedido.complemento = complemento;
  return pedido;
}

/** O que dá para conferir sem a API. Ela valida tudo de novo. */
function validar(valores: Valores): ErrosPorCampo {
  const erros: ErrosPorCampo = {};
  if (!valores.nomeCompleto.trim()) erros.nomeCompleto = "Informe seu nome completo.";
  if (!/^\S+@\S+\.\S+$/.test(valores.email.trim())) erros.email = "Informe um e-mail válido.";
  const telefone = onlyDigits(valores.telefone).length;
  if (telefone < 10 || telefone > 11) erros.telefone = "Informe DDD e número.";
  if (onlyDigits(valores.cep).length !== 8) erros.cep = "O CEP tem 8 dígitos.";
  if (!valores.numero.trim()) erros.numero = "Informe o número.";
  return erros;
}

function errosDaApi(error: unknown): { campos: ErrosPorCampo; geral: string | null } {
  if (error instanceof ApiError && error.status === 503) {
    return { campos: {}, geral: "Não conseguimos consultar o CEP agora. Tente de novo em instantes." };
  }
  if (!(error instanceof ApiError) || isFalhaDeServidor(error)) {
    return { campos: {}, geral: "Não foi possível conectar ao servidor. Tente novamente em instantes." };
  }
  switch (error.status) {
    case 400:
      return { campos: error.fieldErrors as ErrosPorCampo, geral: "Confira os campos destacados." };
    case 409:
      return { campos: { email: "Este e-mail já está em outro cadastro." }, geral: null };
    case 422:
      // CEP que o ViaCEP não conhece, ou usuário sem cadastro de cliente (ADMIN).
      return /cep/i.test(error.message)
        ? { campos: { cep: "Não encontramos esse CEP." }, geral: null }
        : { campos: {}, geral: error.message };
    default:
      return { campos: {}, geral: "Algo deu errado. Tente novamente." };
  }
}

function mensagemDaBusca(error: unknown): string {
  if (error instanceof ApiError && error.status === 422) {
    return "Seu usuário não tem cadastro de cliente.";
  }
  return isFalhaDeServidor(error)
    ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
    : "Não conseguimos carregar seus dados. Tente novamente.";
}

/** Toda a lógica de "Meus dados": busca o cadastro, compara o formulário com ele e envia só o que mudou. */
export function useMeusDados() {
  const logTela = useLogDeTela("meus-dados");
  const [tentativa, setTentativa] = useState(0);
  const [resultado, setResultado] = useState<Resultado | null>(null);
  // `null` = formulário ainda igual ao cadastro (sem edição); a tela usa os valores do cadastro.
  const [edicao, setEdicao] = useState<Valores | null>(null);
  const [erros, setErros] = useState<ErrosPorCampo>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let ativa = true;
    clientesApi.meusDados().then(
      (cliente) => {
        if (ativa) setResultado({ tentativa, cliente });
      },
      (error: unknown) => {
        if (!ativa) return;
        const mensagem = mensagemDaBusca(error);
        logTela.falha("Meus dados não carregados", error, { mensagemExibida: mensagem });
        setResultado({ tentativa, erro: mensagem });
      },
    );
    return () => {
      ativa = false;
    };
  }, [tentativa, logTela]);

  const atual = resultado?.tentativa === tentativa ? resultado : null;
  const cliente = atual && "cliente" in atual ? atual.cliente : null;
  const valores = edicao ?? (cliente ? valoresDe(cliente) : null);
  const alterados = cliente && valores ? Object.keys(mudancas(cliente, valores)) : [];

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    if (!valores) return;
    const campo = event.target.name as Campo;
    const valor = MASCARAS[campo]?.(event.target.value) ?? event.target.value;
    setEdicao({ ...valores, [campo]: valor });
    setErros((anteriores) => ({ ...anteriores, [campo]: undefined }));
    setErroGeral(null);
    setAviso(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!cliente || !valores) return;

    const errosLocais = validar(valores);
    if (Object.keys(errosLocais).length > 0) {
      setErros(errosLocais);
      return;
    }
    const pedido = mudancas(cliente, valores);
    if (Object.keys(pedido).length === 0) {
      setAviso("Nada mudou: seus dados continuam os mesmos.");
      return;
    }

    setEnviando(true);
    setErros({});
    setErroGeral(null);
    logTela.acao("Correção de dados enviada", { campos: Object.keys(pedido) });

    try {
      const atualizado = await clientesApi.atualizarMeusDados(pedido);
      logTela.sucesso("Dados corrigidos", { campos: Object.keys(pedido) });
      setResultado({ tentativa, cliente: atualizado });
      setEdicao(null);
      setAviso("Dados atualizados.");
    } catch (error) {
      const { campos, geral } = errosDaApi(error);
      logTela.falha("Correção de dados não concluída", error, { mensagemExibida: geral ?? Object.values(campos)[0] });
      setErros(campos);
      setErroGeral(geral);
    } finally {
      setEnviando(false);
    }
  }

  /** Props comuns a todo campo: nome, valor, erro e handler. */
  function campo(name: Campo) {
    return { name, value: valores?.[name] ?? "", error: erros[name], onChange: handleChange, disabled: enviando };
  }

  return {
    etapa: (!atual ? "carregando" : "erro" in atual ? "erro" : "pronta") as EtapaMeusDados,
    erroBusca: atual && "erro" in atual ? atual.erro : null,
    tentarDeNovo: () => setTentativa((n) => n + 1),
    cliente,
    campo,
    alterados: alterados.length,
    desfazer: () => {
      setEdicao(null);
      setErros({});
      setErroGeral(null);
      setAviso(null);
    },
    erroGeral,
    aviso,
    enviando,
    handleSubmit,
  };
}

export type MeusDadosState = ReturnType<typeof useMeusDados>;
