import { ArrowRight, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import { MOVIMENTACOES } from "./movimentacoes";
import type { MovimentacaoState } from "./use-movimentacao";

type MovimentacaoFormProps = Pick<MovimentacaoState, "tipo" | "campo" | "erroGeral" | "enviando" | "handleSubmit">;

export function MovimentacaoForm({ tipo, campo, erroGeral, enviando, handleSubmit }: MovimentacaoFormProps) {
  const config = MOVIMENTACOES[tipo];

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
      {tipo === "transferencia" && (
        <FormField
          label="ID da conta de destino"
          placeholder="Cole o ID que a outra pessoa te passou"
          autoComplete="off"
          spellCheck={false}
          className="[&_input]:font-mono [&_input]:text-sm"
          {...campo("contaDestinoId")}
        />
      )}

      <FormField
        label="Valor"
        placeholder="R$ 0,00"
        inputMode="numeric"
        autoComplete="off"
        className="[&_input]:text-lg [&_input]:tabular-nums"
        {...campo("valor")}
      />

      <FormField label="Descrição" hint="Opcional" maxLength={140} autoComplete="off" {...campo("descricao")} />

      <div className="mt-5 flex flex-col gap-4">
        {erroGeral && <FormAlert>{erroGeral}</FormAlert>}

        <Button type="submit" variant="brand" size="xl" disabled={enviando} className="h-[3.25rem] w-full">
          {enviando ? (
            <>
              <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
              {config.rotuloEnviando}
            </>
          ) : (
            <>
              {config.rotuloBotao}
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
