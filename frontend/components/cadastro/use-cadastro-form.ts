import { useState, type ChangeEvent, type FormEvent } from "react";
import {
  ApiError,
  authApi,
  clientesApi,
  isFalhaDeServidor,
  type CadastroClienteRequest,
  type ClienteResponse,
} from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { maskCep, maskCpf, maskTelefone, onlyDigits } from "@/lib/masks";

/** Campos da tela: os do contrato mais a confirmação da senha, que não vai para a API. */
export type Campo = keyof CadastroClienteRequest | "confirmarSenha";
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
  senha: "",
  confirmarSenha: "",
};

const CAMPOS_OBRIGATORIOS: readonly Campo[] = [
  "nomeCompleto",
  "cpf",
  "telefone",
  "email",
  "cep",
  "numero",
  "senha",
  "confirmarSenha",
];

/** Limites do contrato (3.1): o BCrypt do backend só usa os primeiros 72 bytes, e acento ocupa 2. */
const SENHA_MINIMO = 8;
const SENHA_MAXIMO_BYTES = 72;

/** Confere a senha antes de enviar; o resto dos campos o backend valida. */
function validarSenha(valores: Valores): ErrosPorCampo {
  const erros: ErrosPorCampo = {};
  if (valores.senha.length < SENHA_MINIMO) {
    erros.senha = `Use pelo menos ${SENHA_MINIMO} caracteres.`;
  } else if (new TextEncoder().encode(valores.senha).length > SENHA_MAXIMO_BYTES) {
    erros.senha = "Senha longa demais. Use até 72 caracteres (acentos contam como 2).";
  }
  if (valores.confirmarSenha !== valores.senha) {
    erros.confirmarSenha = "As senhas não são iguais.";
  }
  return erros;
}

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
    // A senha vai como foi digitada: espaços também fazem parte dela.
    senha: valores.senha,
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
  /** O login automático depois do cadastro deu certo? Se não, a tela de sucesso manda para o login. */
  const [logado, setLogado] = useState(false);

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

    const errosDeSenha = validarSenha(valores);
    if (Object.keys(errosDeSenha).length > 0) {
      setErros(errosDeSenha);
      setErroGeral(null);
      return;
    }

    setEnviando(true);
    setErros({});
    setErroGeral(null);

    logTela.acao("Formulário enviado");

    try {
      const request = toRequest(valores);
      const novoCliente = await clientesApi.cadastrar(request);
      logTela.sucesso("Cadastro concluído", { clienteId: novoCliente.id });
      // O cadastro não devolve token (contrato 3.1): entra com o CPF e a senha que a pessoa acabou de criar.
      setLogado(await entrarDepoisDoCadastro(request.cpf, request.senha));
      setCliente(novoCliente);
      // A senha não precisa mais ficar na memória da tela (o nome continua no cartão da vitrine).
      setValores((atual) => ({ ...atual, senha: "", confirmarSenha: "" }));
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

  /** Falhar aqui não desfaz o cadastro: a pessoa só precisa entrar pela tela de login. */
  async function entrarDepoisDoCadastro(cpf: string, senha: string): Promise<boolean> {
    try {
      await authApi.login({ cpf, senha });
      return true;
    } catch (error) {
      logTela.falha("Login automático depois do cadastro falhou", error);
      return false;
    }
  }

  const preenchido = (nome: Campo) => valores[nome].trim() !== "";
  const preenchidos = CAMPOS_OBRIGATORIOS.filter(preenchido).length;

  return {
    nome: valores.nomeCompleto.trim(),
    progresso: preenchidos / CAMPOS_OBRIGATORIOS.length,
    /** Cada bloco do formulário com todos os obrigatórios preenchidos (os indicadores "acendem"). */
    etapasCompletas: {
      dados: (["nomeCompleto", "cpf", "telefone", "email"] as const).every(preenchido),
      endereco: (["cep", "numero"] as const).every(preenchido),
      acesso: (["senha", "confirmarSenha"] as const).every(preenchido),
    },
    /** Só para o medidor de força: a senha nunca sai do hook por outro caminho. */
    senhaDigitada: valores.senha,
    erroGeral,
    enviando,
    cliente,
    logado,
    campo,
    handleSubmit,
  };
}

export type CadastroFormState = ReturnType<typeof useCadastroForm>;
