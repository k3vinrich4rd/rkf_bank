import type { ReactNode } from "react";
import { ArrowRight, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import type { CadastroFormState } from "./use-cadastro-form";

type CadastroFormProps = Pick<CadastroFormState, "campo" | "handleSubmit" | "enviando" | "erroGeral">;

export function CadastroForm({ campo, handleSubmit, enviando, erroGeral }: CadastroFormProps) {
  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-10">
      <FormSection step="01" title="Dados pessoais">
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

      <FormSection step="02" title="Endereço" description="Completamos rua, bairro e cidade pelo CEP.">
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

      <FormSection step="03" title="Acesso" description="Você entra com seu CPF e esta senha.">
        <FormField
          {...campo("senha")}
          label="Senha"
          hint="Mínimo de 8 caracteres"
          type="password"
          autoComplete="new-password"
          className="sm:col-span-3"
        />
        <FormField
          {...campo("confirmarSenha")}
          label="Confirme a senha"
          type="password"
          autoComplete="new-password"
          className="sm:col-span-3"
        />
      </FormSection>

      <div className="flex flex-col gap-4">
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

        <p className="text-center text-xs leading-relaxed text-muted-foreground/70">
          Ao continuar, você concorda com os Termos de Uso e a Política de Privacidade do RKF.
        </p>
      </div>
    </form>
  );
}

interface FormSectionProps {
  step: string;
  title: string;
  description?: string;
  children: ReactNode;
}

/** Grupo numerado de campos. `fieldset` + `legend` dão semântica ao grupo. */
function FormSection({ step, title, description, children }: FormSectionProps) {
  return (
    <fieldset>
      <legend className="flex w-full items-center gap-3">
        <span className="font-mono text-xs text-brand-cyan">{step}</span>
        <span className="text-sm font-medium">{title}</span>
        <span aria-hidden="true" className="h-px flex-1 bg-gradient-to-r from-border to-transparent" />
      </legend>

      {description && <p className="mt-2 text-xs text-muted-foreground">{description}</p>}

      <div className="mt-5 grid gap-5 sm:grid-cols-6">{children}</div>
    </fieldset>
  );
}
