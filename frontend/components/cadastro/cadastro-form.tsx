import type { ReactNode } from "react";
import Link from "next/link";
import { ArrowRight, Check, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import { PasswordField } from "@/components/ui/password-field";
import { ROUTES } from "@/lib/routes";
import { ForcaSenha } from "./forca-senha";
import type { CadastroFormState } from "./use-cadastro-form";

type CadastroFormProps = Pick<
  CadastroFormState,
  "campo" | "handleSubmit" | "enviando" | "erroGeral" | "etapasCompletas" | "senhaDigitada" | "termos"
>;

export function CadastroForm({ campo, handleSubmit, enviando, erroGeral, etapasCompletas, senhaDigitada, termos }: CadastroFormProps) {
  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-10">
      <FormSection step="01" title="Dados pessoais" completa={etapasCompletas.dados}>
        <FormField
          {...campo("nomeCompleto")}
          label="Nome completo"
          placeholder="Como está no seu documento"
          autoComplete="name"
          autoFocus
          className="sm:col-span-6"
        />
        <FormField
          {...campo("cpf")}
          label="CPF"
          placeholder="000.000.000-00"
          inputMode="numeric"
          className="sm:col-span-3"
        />
        <FormField
          {...campo("telefone")}
          label="Celular"
          placeholder="(00) 00000-0000"
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          className="sm:col-span-3"
        />
        <FormField
          {...campo("email")}
          label="E-mail"
          placeholder="voce@email.com"
          type="email"
          autoComplete="email"
          className="sm:col-span-6"
        />
      </FormSection>

      <FormSection step="02" title="Endereço" description="Completamos rua, bairro e cidade pelo CEP." completa={etapasCompletas.endereco}>
        <FormField
          {...campo("cep")}
          label="CEP"
          placeholder="00000-000"
          inputMode="numeric"
          autoComplete="postal-code"
          className="sm:col-span-3"
        />
        <FormField
          {...campo("numero")}
          label="Número"
          placeholder="123"
          className="sm:col-span-3"
        />
        <FormField
          {...campo("complemento")}
          label="Complemento"
          hint="Opcional"
          placeholder="Apto, bloco, casa…"
          className="sm:col-span-6"
        />
      </FormSection>

      <FormSection step="03" title="Acesso" description="Você entra com seu CPF e esta senha." completa={etapasCompletas.acesso}>
        <PasswordField
          {...campo("senha")}
          label="Senha"
          hint="8 a 72 caracteres"
          autoComplete="new-password"
          className="sm:col-span-6"
        >
          <ForcaSenha senha={senhaDigitada} />
        </PasswordField>
        <PasswordField
          {...campo("confirmarSenha")}
          label="Confirme a senha"
          autoComplete="new-password"
          className="sm:col-span-6"
        />
      </FormSection>

      <div className="flex flex-col gap-4">
        <AceiteTermos {...termos} />

        {erroGeral && <FormAlert>{erroGeral}</FormAlert>}

        <Button type="submit" variant="brand" size="xl" disabled={enviando} className="h-[3.25rem] w-full">
          {enviando ? (
            <>
              <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
              Criando seu cadastro…
            </>
          ) : (
            <>
              Criar meu cadastro
              <ArrowRight
                data-icon="inline-end"
                aria-hidden="true"
                className="transition-transform group-hover/button:translate-x-0.5"
              />
            </>
          )}
        </Button>

      </div>
    </form>
  );
}

interface AceiteTermosProps {
  aceito: boolean;
  alternar: (aceito: boolean) => void;
  erro?: string;
  disabled: boolean;
}

/**
 * Aceite dos termos (LGPD, contrato 3.1): obrigatório e registrado pela API com a versão e a data.
 * Os links abrem em outra aba para a pessoa ler sem perder o que já preencheu.
 */
function AceiteTermos({ aceito, alternar, erro, disabled }: AceiteTermosProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="flex cursor-pointer items-start gap-3 text-sm leading-relaxed text-muted-foreground">
        <input
          type="checkbox"
          name="aceiteTermos"
          checked={aceito}
          disabled={disabled}
          onChange={(event) => alternar(event.target.checked)}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? "aceiteTermos-erro" : undefined}
          className="mt-0.5 size-4 shrink-0 accent-brand-cyan"
        />
        <span>
          Li e aceito os{" "}
          <Link href={ROUTES.termos} target="_blank" className="text-foreground underline underline-offset-4">
            Termos de Uso
          </Link>{" "}
          e a{" "}
          <Link href={ROUTES.privacidade} target="_blank" className="text-foreground underline underline-offset-4">
            Política de Privacidade
          </Link>{" "}
          do RKF.
        </span>
      </label>
      {erro && (
        <p id="aceiteTermos-erro" role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </div>
  );
}

interface FormSectionProps {
  step: string;
  title: string;
  description?: string;
  /** Todos os obrigatórios do bloco preenchidos: o número vira um ✓. */
  completa?: boolean;
  children: ReactNode;
}

/** Grupo numerado de campos. `fieldset` + `legend` dão semântica ao grupo. */
function FormSection({ step, title, description, completa = false, children }: FormSectionProps) {
  return (
    <fieldset>
      <legend className="flex w-full items-center gap-3">
        <span
          className={`grid size-6 place-items-center rounded-full border font-mono text-[0.65rem] transition-colors duration-300 ${
            completa ? "border-brand-cyan/50 bg-brand-cyan/15 text-brand-cyan" : "border-white/10 text-muted-foreground"
          }`}
        >
          {completa ? <Check className="size-3" strokeWidth={3} aria-label="completo" /> : step}
        </span>
        <span className="text-sm font-medium">{title}</span>
        <span aria-hidden="true" className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
      </legend>

      {description && <p className="mt-2 text-xs text-muted-foreground">{description}</p>}

      <div className="mt-5 grid gap-5 sm:grid-cols-6">{children}</div>
    </fieldset>
  );
}
