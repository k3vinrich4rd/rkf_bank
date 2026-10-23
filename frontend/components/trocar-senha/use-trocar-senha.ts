import { useState, type ChangeEvent, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ApiError, authApi, isFalhaDeServidor } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";
import { ROUTES } from "@/lib/routes";

export type Campo = "senhaAtual" | "novaSenha" | "confirmarNovaSenha";
type Valores = Record<Campo, string>;
type ErrosPorCampo = Partial<Record<Campo, string>>;

const VALORES_INICIAIS: Valores = { senhaAtual: "", novaSenha: "", confirmarNovaSenha: "" };
const MIN_SENHA = 8;
const MAX_BYTES_SENHA = 72;

/** O que dá para conferir sem ir à API. A API valida tudo de novo (contrato 2.3, R22). */
function validar(valores: Valores): ErrosPorCampo {
  const erros: ErrosPorCampo = {};
  if (!valores.senhaAtual) erros.senhaAtual = "Informe a sua senha atual.";

  // O BCrypt da API conta bytes (acento ocupa 2), não caracteres.
  const bytes = new TextEncoder().encode(valores.novaSenha).length;
  if (valores.novaSenha.length < MIN_SENHA) erros.novaSenha = `A nova senha precisa ter pelo menos ${MIN_SENHA} caracteres.`;
  else if (bytes > MAX_BYTES_SENHA) erros.novaSenha = "A nova senha está longa demais.";
  else if (valores.novaSenha === valores.senhaAtual) erros.novaSenha = "A nova senha precisa ser diferente da atual.";

  if (valores.confirmarNovaSenha !== valores.novaSenha) erros.confirmarNovaSenha = "As duas senhas não são iguais.";
  return erros;
}

/** Traduz a resposta da API em erro de campo ou mensagem geral. */
function errosDaApi(error: unknown): { campos: ErrosPorCampo; geral: string | null } {
  if (!(error instanceof ApiError) || isFalhaDeServidor(error)) {
    return { campos: {}, geral: "Não foi possível conectar ao servidor. Tente novamente em instantes." };
  }
  switch (error.status) {
    case 400:
      return { campos: error.fieldErrors as ErrosPorCampo, geral: "Confira os campos destacados." };
    case 422:
      return error.codigo === "SENHA_ATUAL_INCORRETA"
        ? { campos: { senhaAtual: "Senha atual incorreta." }, geral: null }
        : { campos: { novaSenha: "A nova senha precisa ser diferente da atual." }, geral: null };
    case 429:
      return { campos: {}, geral: "Muitas tentativas com a senha errada. Por segurança, aguarde alguns minutos." };
    default:
      return { campos: {}, geral: "Algo deu errado. Tente novamente." };
  }
}

/**
 * Toda a lógica da troca de senha: campos, validação, envio e o que fazer depois.
 * Depois do 204 a sessão acabou (a API derruba os tokens antigos), então vai para o login com um aviso.
 */
export function useTrocarSenha() {
  const logTela = useLogDeTela("trocar-senha");
  const router = useRouter();
  const [valores, setValores] = useState<Valores>(VALORES_INICIAIS);
  const [erros, setErros] = useState<ErrosPorCampo>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const campo = event.target.name as Campo;
    setValores((atual) => ({ ...atual, [campo]: event.target.value }));
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
    logTela.acao("Troca de senha enviada");

    try {
      await authApi.trocarSenha({ senhaAtual: valores.senhaAtual, novaSenha: valores.novaSenha });
      logTela.sucesso("Senha trocada");
      // `replace`: voltar no navegador não deve trazer de volta uma tela que exige a sessão que acabou de cair.
      router.replace(`${ROUTES.login}?senha=trocada`);
    } catch (error) {
      // 401: o token já não vale (venceu, ou outro aparelho trocou a senha). O `apiRequest` não redireciona nas
      // rotas `/auth/*`, então leva ao login aqui.
      if (error instanceof ApiError && error.status === 401) {
        router.replace(`${ROUTES.login}?expirou=1&voltar=${encodeURIComponent(ROUTES.trocarSenha)}`);
        return;
      }
      const { campos, geral } = errosDaApi(error);
      logTela.falha("Troca de senha não concluída", error, { mensagemExibida: geral ?? Object.values(campos)[0] });
      setErros(campos);
      setErroGeral(geral);
      // Nunca deixa senha digitada esperando na tela depois de um erro.
      setValores(VALORES_INICIAIS);
      setEnviando(false);
    }
  }

  /** Props comuns a todo campo: nome, valor, erro e handler. */
  function campo(name: Campo) {
    return { name, value: valores[name], error: erros[name], onChange: handleChange, disabled: enviando };
  }

  return { campo, erroGeral, enviando, handleSubmit };
}

export type TrocarSenhaState = ReturnType<typeof useTrocarSenha>;
