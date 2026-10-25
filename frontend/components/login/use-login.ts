import { useState, type ChangeEvent, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ApiError, authApi, isFalhaDeServidor, type LoginRequest } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { ROUTES } from "@/lib/routes";
import { maskCpf, onlyDigits } from "@/lib/masks";

export type Campo = keyof LoginRequest;
type Valores = Record<Campo, string>;
type ErrosPorCampo = Partial<Record<Campo, string>>;

/** Traduz qualquer falha do login numa mensagem para o usuário. */
function mensagemDeErro(error: unknown): string {
  if (!(error instanceof ApiError) || isFalhaDeServidor(error)) {
    return "Não foi possível conectar ao servidor. Tente novamente em instantes.";
  }

  switch (error.status) {
    case 400:
      return "Confira os campos destacados.";
    // A API responde igual para CPF sem cadastro e senha errada, de propósito (não revela quem tem conta).
    case 401:
      return "CPF ou senha inválidos.";
    // 5 senhas erradas para o mesmo CPF em 15 min: a API bloqueia aquele CPF (contrato 2.1, R17).
    case 429:
      return "Muitas tentativas seguidas. Por segurança, aguarde alguns minutos e tente de novo.";
    default:
      return "Algo deu errado. Tente novamente.";
  }
}

function validar(valores: Valores): ErrosPorCampo {
  const erros: ErrosPorCampo = {};
  if (onlyDigits(valores.cpf).length !== 11) erros.cpf = "Informe os 11 dígitos do seu CPF.";
  if (!valores.senha) erros.senha = "Informe sua senha.";
  return erros;
}

/**
 * Toda a lógica do login: campos, envio, erros e o redirecionamento.
 * `destino` já vem validado pela página (`destinoDepoisDoLogin`).
 */
export function useLogin(destino: string) {
  const logTela = useLogDeTela("login");
  const router = useRouter();
  const [valores, setValores] = useState<Valores>({ cpf: "", senha: "" });
  const [erros, setErros] = useState<ErrosPorCampo>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const campo = event.target.name as Campo;
    const valor = campo === "cpf" ? maskCpf(event.target.value) : event.target.value;

    setValores((atual) => ({ ...atual, [campo]: valor }));
    setErros((atual) => ({ ...atual, [campo]: undefined }));
    setErroGeral(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const errosLocais = validar(valores);
    if (Object.keys(errosLocais).length > 0) {
      setErros(errosLocais);
      return;
    }

    setEnviando(true);
    setErros({});
    setErroGeral(null);
    logTela.acao("Login enviado");

    try {
      const sessao = await authApi.login({ cpf: onlyDigits(valores.cpf), senha: valores.senha });
      // Senha temporária (redefinida pelo ADMIN): a API só libera a troca de senha, então vai direto para ela.
      const proximaTela = sessao.deveTrocarSenha ? `${ROUTES.trocarSenha}?temporaria=1` : destino;
      logTela.sucesso("Login concluído", { destino: proximaTela });
      // `replace`: voltar no navegador não deve trazer de novo a tela de login.
      router.replace(proximaTela);
    } catch (error) {
      const mensagem = mensagemDeErro(error);
      logTela.falha("Login não concluído", error, { mensagemExibida: mensagem });
      if (error instanceof ApiError) setErros(error.fieldErrors);
      // Senha errada: limpa só a senha, o CPF continua.
      setValores((atual) => ({ ...atual, senha: "" }));
      setErroGeral(mensagem);
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

  return { campo, erroGeral, enviando, handleSubmit };
}

export type LoginState = ReturnType<typeof useLogin>;
