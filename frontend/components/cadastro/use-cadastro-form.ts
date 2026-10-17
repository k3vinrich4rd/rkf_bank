import { useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError, clientesApi, isFalhaDeServidor, type CadastroClienteRequest, type ClienteResponse } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { salvarContaAtual } from "@/lib/conta-atual";
import { maskCep, maskCpf, maskTelefone, onlyDigits } from "@/lib/masks";

export type Campo = keyof CadastroClienteRequest;
type Valores = Record<Campo, string>;
type ErrosPorCampo = Partial<Record<Campo, string>>;

const VALORES_INICIAIS: Valores = {
  nomeCompleto: "",
  cpf: "",
  email: "",
  telefone: "",
  cep: "",
  numero: "",
  complemento: "",
};

const CAMPOS_OBRIGATORIOS: readonly Campo[] = [
  "nomeCompleto",
  "cpf",
  "telefone",
  "email",
  "cep",
  "numero",
];

const MASCARAS: Partial<Record<Campo, (valor: string) => string>> = {
  cpf: maskCpf,
  telefone: maskTelefone,
  cep: maskCep,
};

/** Converte o que está na tela para o formato que a API espera. */
function toRequest(valores: Valores): CadastroClienteRequest {
  return {
    nomeCompleto: valores.nomeCompleto.trim(),
    cpf: onlyDigits(valores.cpf),
    email: valores.email.trim(),
    telefone: onlyDigits(valores.telefone),
    cep: onlyDigits(valores.cep),
    numero: valores.numero.trim(),
    complemento: valores.complemento.trim() || undefined,
  };
}

/** Traduz qualquer falha do envio numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
  // 503 é o ViaCEP fora do ar (vem do backend); qualquer outra falha de servidor é conexão.
  if (error instanceof ApiError && error.status === 503) {
    return "Não conseguimos consultar o seu CEP agora. Tente novamente em instantes.";
  }
  if (!(error instanceof ApiError) || isFalhaDeServidor(error)) {
    return "Não foi possível conectar ao servidor. Tente novamente em instantes.";
  }

  switch (error.status) {
    case 400:
      return "Confira os campos destacados.";
    case 409: // CPF ou e-mail já cadastrado
    case 422: // CEP inexistente
      return error.message;
    default:
      return "Algo deu errado. Tente novamente.";
  }
}

/**
 * Toda a lógica do cadastro: estado dos campos, máscaras, envio e erros.
 * Os componentes só desenham — não sabem nada de API.
 */
export function useCadastroForm() {
  const logTela = useLogDeTela("cadastro");
  const [valores, setValores] = useState<Valores>(VALORES_INICIAIS);
  const [erros, setErros] = useState<ErrosPorCampo>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [cliente, setCliente] = useState<ClienteResponse | null>(null);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const campo = event.target.name as Campo;
    const aplicarMascara = MASCARAS[campo];
    const valor = aplicarMascara ? aplicarMascara(event.target.value) : event.target.value;

    setValores((atual) => ({ ...atual, [campo]: valor }));
    // O usuário está corrigindo o formulário: os erros antigos deixam de valer.
    setErros((atual) => ({ ...atual, [campo]: undefined }));
    setErroGeral(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEnviando(true);
    setErros({});
    setErroGeral(null);

    logTela.acao("Formulário enviado");

    try {
      const novoCliente = await clientesApi.cadastrar(toRequest(valores));
      logTela.sucesso("Cadastro concluído", { clienteId: novoCliente.id });
      // Lembra o cliente no navegador para a abertura de conta (ainda não há login).
      salvarContaAtual({ clienteId: novoCliente.id });
      setCliente(novoCliente);
    } catch (error) {
      // O detalhe técnico já foi logado na camada HTTP; aqui registramos o que o usuário viu.
      const mensagem = mensagemDeErro(error);
      const camposComErro = error instanceof ApiError ? Object.keys(error.fieldErrors) : [];
      logTela.falha("Cadastro não concluído", error, {
        mensagemExibida: mensagem,
        ...(camposComErro.length > 0 && { camposComErro }),
      });

      if (error instanceof ApiError) setErros(error.fieldErrors);
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

  const preenchidos = CAMPOS_OBRIGATORIOS.filter((nome) => valores[nome].trim()).length;

  return {
    nome: valores.nomeCompleto.trim(),
    progresso: preenchidos / CAMPOS_OBRIGATORIOS.length,
    erroGeral,
    enviando,
    cliente,
    campo,
    handleSubmit,
  };
}

export type CadastroFormState = ReturnType<typeof useCadastroForm>;
