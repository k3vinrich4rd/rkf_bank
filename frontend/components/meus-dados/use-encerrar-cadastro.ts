import { useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError, authApi, clientesApi, isFalhaDeServidor, type EncerramentoResponse } from "@/lib/api";
import { useLogDeTela } from "@/components/logs";

/**
 * Encerrar o cadastro (LGPD, contrato 2.5): ação irreversível, então pede dois passos (abrir a confirmação e
 * digitar a senha). Depois do `200`, todo token do titular dá `401`: a sessão é apagada aqui mesmo.
 */
export function useEncerrarCadastro() {
  const logTela = useLogDeTela("encerrar-cadastro");
  const [confirmando, setConfirmando] = useState(false);
  const [senha, setSenha] = useState("");
  const [erroSenha, setErroSenha] = useState<string | null>(null);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [encerramento, setEncerramento] = useState<EncerramentoResponse | null>(null);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    setSenha(event.target.value);
    setErroSenha(null);
    setErroGeral(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!senha) {
      setErroSenha("Digite sua senha para confirmar.");
      return;
    }

    setEnviando(true);
    setErroGeral(null);
    logTela.acao("Encerramento de cadastro enviado");

    try {
      const resposta = await clientesApi.encerrarCadastro({ senha });
      logTela.sucesso("Cadastro encerrado");
      try {
        await authApi.sair();
      } catch (error) {
        // O token já não vale na API; o cookie que sobrar só leva ao login na próxima tela.
        logTela.falha("Sessão não apagada depois do encerramento", error);
      }
      setEncerramento(resposta);
    } catch (error) {
      if (error instanceof ApiError && error.status === 422 && /senha/i.test(error.message)) {
        setErroSenha("Senha incorreta.");
      } else if (error instanceof ApiError && error.status === 422) {
        // Saldo nas contas: o dinheiro não some, precisa sair antes.
        setErroGeral("Transfira ou saque o saldo das suas contas antes de encerrar o cadastro.");
      } else if (error instanceof ApiError && error.status === 429) {
        setErroGeral("Muitas tentativas com a senha errada. Por segurança, aguarde alguns minutos.");
      } else {
        setErroGeral(
          isFalhaDeServidor(error)
            ? "Não foi possível conectar ao servidor. Tente novamente em instantes."
            : "Algo deu errado. Tente novamente.",
        );
      }
      logTela.falha("Encerramento não concluído", error);
    } finally {
      // Nunca deixa senha digitada esperando na tela.
      setSenha("");
      setEnviando(false);
    }
  }

  return {
    confirmando,
    abrirConfirmacao: () => setConfirmando(true),
    cancelar: () => {
      setConfirmando(false);
      setSenha("");
      setErroSenha(null);
      setErroGeral(null);
    },
    senha: { name: "senha", value: senha, error: erroSenha ?? undefined, onChange: handleChange, disabled: enviando },
    erroGeral,
    enviando,
    encerramento,
    handleSubmit,
  };
}

export type EncerrarCadastroState = ReturnType<typeof useEncerrarCadastro>;
