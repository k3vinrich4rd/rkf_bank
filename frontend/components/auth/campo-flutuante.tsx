"use client";

import { useState, type ComponentProps, type KeyboardEvent } from "react";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";

interface CampoFlutuanteProps extends Omit<ComponentProps<"input">, "placeholder"> {
  name: string;
  label: string;
  error?: string;
  /** Campo de senha: ganha "mostrar/ocultar" e o aviso de Caps Lock. */
  senha?: boolean;
}

/**
 * Campo com rótulo flutuante: o rótulo fica dentro do campo e sobe quando ele ganha foco ou valor.
 * O truque é o `placeholder=" "`: com o campo vazio o placeholder "aparece" (`:placeholder-shown`) e o rótulo
 * fica no meio; com valor, ele some e o rótulo sobe. Continua sendo um `<label htmlFor>` de verdade.
 */
export function CampoFlutuante({ name, label, error, senha = false, className, type, ...props }: CampoFlutuanteProps) {
  const [visivel, setVisivel] = useState(false);
  const [capsLock, setCapsLock] = useState(false);
  const idErro = `${name}-erro`;
  const idCaps = `${name}-caps`;
  const conferirCaps = (event: KeyboardEvent<HTMLInputElement>) => setCapsLock(event.getModifierState("CapsLock"));

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="relative">
        <input
          id={name}
          name={name}
          type={senha ? (visivel ? "text" : "password") : type}
          placeholder=" "
          aria-invalid={error ? true : undefined}
          aria-describedby={cn(error && idErro, capsLock && idCaps) || undefined}
          onKeyDown={senha ? conferirCaps : undefined}
          onKeyUp={senha ? conferirCaps : undefined}
          onBlur={() => setCapsLock(false)}
          className={cn(
            "peer h-[3.75rem] w-full rounded-2xl border border-white/[0.08] bg-white/[0.025] px-4 pt-5 text-[0.95rem] text-foreground outline-none",
            "shadow-[inset_0_1px_0_rgb(255_255_255/0.03)] transition-[border-color,background-color,box-shadow] duration-300",
            "hover:border-white/[0.14] focus:border-brand-cyan/50 focus:bg-white/[0.04] focus:shadow-[0_0_0_4px_rgb(6_182_212/0.08)]",
            "aria-invalid:border-destructive/60 aria-invalid:shadow-[0_0_0_4px_rgb(239_68_68/0.08)]",
            "disabled:cursor-not-allowed disabled:opacity-60",
            senha && "pr-12",
          )}
          {...props}
        />
        <label
          htmlFor={name}
          className={cn(
            "pointer-events-none absolute top-1/2 left-4 origin-left -translate-y-1/2 text-[0.95rem] text-muted-foreground transition-all duration-200",
            "peer-focus:top-[1.1rem] peer-focus:scale-[0.78] peer-focus:text-brand-cyan",
            "peer-[:not(:placeholder-shown)]:top-[1.1rem] peer-[:not(:placeholder-shown)]:scale-[0.78]",
          )}
        >
          {label}
        </label>
        {senha && (
          <button
            type="button"
            onClick={() => setVisivel((atual) => !atual)}
            aria-label={visivel ? "Ocultar senha" : "Mostrar senha"}
            aria-pressed={visivel}
            disabled={props.disabled}
            className="absolute inset-y-0 right-0 grid w-12 place-items-center text-muted-foreground transition-colors hover:text-foreground focus-visible:text-foreground focus-visible:outline-none disabled:opacity-50"
          >
            {visivel ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
          </button>
        )}
      </div>

      {capsLock && (
        <p id={idCaps} className="px-1 text-xs text-amber-400/90 animate-in fade-in duration-200">
          Caps Lock está ligado.
        </p>
      )}
      {error && (
        <p id={idErro} className="px-1 text-xs text-destructive animate-in fade-in slide-in-from-top-1 duration-200">
          {error}
        </p>
      )}
    </div>
  );
}
