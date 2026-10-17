import { ArrowRight, Check, LoaderCircle, PiggyBank, Wallet, type LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import type { TipoConta } from "@/lib/api";
import { formatTipoConta } from "@/lib/format";
import type { AbrirContaState } from "./use-abrir-conta";

interface OpcaoTipoConta {
  tipo: TipoConta;
  descricao: string;
  icone: LucideIcon;
}

const OPCOES: readonly OpcaoTipoConta[] = [
  { tipo: "CORRENTE", descricao: "Para o dia a dia: receber, pagar e transferir.", icone: Wallet },
  { tipo: "POUPANCA", descricao: "Para separar o dinheiro que você quer guardar.", icone: PiggyBank },
];

type AbrirContaFormProps = Pick<
  AbrirContaState,
  "tipoConta" | "escolherTipo" | "handleSubmit" | "enviando" | "erroGeral"
>;

export function AbrirContaForm({ tipoConta, escolherTipo, handleSubmit, enviando, erroGeral }: AbrirContaFormProps) {
  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-10">
      <fieldset disabled={enviando}>
        <legend className="sr-only">Tipo de conta</legend>
        <div className="grid gap-3">
          {OPCOES.map((opcao) => (
            <OpcaoCard
              key={opcao.tipo}
              opcao={opcao}
              selecionada={tipoConta === opcao.tipo}
              onSelecionar={escolherTipo}
            />
          ))}
        </div>
      </fieldset>

      <div className="flex flex-col gap-4">
        {erroGeral && <FormAlert>{erroGeral}</FormAlert>}

        <Button type="submit" variant="brand" size="xl" disabled={enviando} className="h-[3.25rem] w-full">
          {enviando ? (
            <>
              <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
              Abrindo sua conta…
            </>
          ) : (
            <>
              Abrir {formatTipoConta(tipoConta).toLowerCase()}
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

interface OpcaoCardProps {
  opcao: OpcaoTipoConta;
  selecionada: boolean;
  onSelecionar: (tipo: TipoConta) => void;
}

/** Radio nativo escondido dentro de um card: mantém teclado e leitor de tela funcionando. */
function OpcaoCard({ opcao, selecionada, onSelecionar }: OpcaoCardProps) {
  const { tipo, descricao, icone: Icone } = opcao;

  return (
    <label className="group flex cursor-pointer items-center gap-4 rounded-2xl border border-white/[0.08] bg-white/[0.02] p-5 transition-colors hover:border-white/[0.16] has-checked:border-brand-cyan/50 has-checked:bg-brand-blue/[0.06] has-focus-visible:ring-3 has-focus-visible:ring-ring/50 has-disabled:cursor-not-allowed has-disabled:opacity-60">
      <input
        type="radio"
        name="tipoConta"
        value={tipo}
        checked={selecionada}
        onChange={() => onSelecionar(tipo)}
        className="sr-only"
      />

      <span className="grid size-11 shrink-0 place-items-center rounded-xl border border-white/[0.08] text-muted-foreground transition-colors group-has-checked:border-brand-cyan/30 group-has-checked:text-brand-cyan">
        <Icone className="size-5" aria-hidden="true" />
      </span>

      <span className="flex-1">
        <span className="block font-medium">{formatTipoConta(tipo)}</span>
        <span className="mt-1 block text-sm text-muted-foreground">{descricao}</span>
      </span>

      <span
        aria-hidden="true"
        className="grid size-5 shrink-0 place-items-center rounded-full border border-white/[0.16] text-white transition-colors group-has-checked:border-transparent group-has-checked:bg-gradient-brand"
      >
        {selecionada && <Check className="size-3" strokeWidth={3} />}
      </span>
    </label>
  );
}
