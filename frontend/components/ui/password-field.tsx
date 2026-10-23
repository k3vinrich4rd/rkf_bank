"use client"

import * as React from "react"
import { Eye, EyeOff } from "lucide-react"
import { cn } from "cn"
import { Input } from "@/components/ui/input"

interface PasswordFieldProps extends Omit<React.ComponentProps<typeof Input>, "type"> {
  name: string
  label: string
  hint?: string
  error?: string
  /** Conteúdo logo abaixo do campo (ex.: medidor de força). */
  children?: React.ReactNode
}

/**
 * Campo de senha com "mostrar/ocultar" e aviso de Caps Lock — os dois erros mais comuns de quem digita senha.
 * Mesmo contrato do `FormField` (label, hint, error ligados por `id` e `aria-*`).
 */
function PasswordField({ name, label, hint, error, className, children, onKeyDown, onKeyUp, onBlur, ...props }: PasswordFieldProps) {
  const [visivel, setVisivel] = React.useState(false)
  const [capsLock, setCapsLock] = React.useState(false)
  const errorId = `${name}-error`
  const capsId = `${name}-caps`

  const conferirCapsLock = (event: React.KeyboardEvent<HTMLInputElement>) => {
    setCapsLock(event.getModifierState("CapsLock"))
  }

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <label htmlFor={name} className="flex items-baseline justify-between text-[0.8rem] font-medium text-foreground/80">
        {label}
        {hint && <span className="text-xs font-normal text-muted-foreground/70">{hint}</span>}
      </label>

      <div className="relative">
        <Input
          id={name}
          name={name}
          type={visivel ? "text" : "password"}
          aria-invalid={error ? true : undefined}
          aria-describedby={cn(error && errorId, capsLock && capsId) || undefined}
          className="pr-12"
          onKeyDown={(event) => {
            conferirCapsLock(event)
            onKeyDown?.(event)
          }}
          onKeyUp={(event) => {
            conferirCapsLock(event)
            onKeyUp?.(event)
          }}
          onBlur={(event) => {
            setCapsLock(false)
            onBlur?.(event)
          }}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisivel((atual) => !atual)}
          aria-label={visivel ? "Ocultar senha" : "Mostrar senha"}
          aria-pressed={visivel}
          disabled={props.disabled}
          className="absolute inset-y-0 right-0 grid w-12 place-items-center rounded-r-xl text-muted-foreground transition-colors hover:text-foreground focus-visible:text-foreground focus-visible:outline-none disabled:opacity-50"
        >
          {visivel ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
        </button>
      </div>

      {children}

      {capsLock && (
        <p id={capsId} className="text-xs text-amber-400/90 animate-in fade-in duration-200">
          Caps Lock está ligado.
        </p>
      )}
      {error && (
        <p id={errorId} className="text-xs text-destructive animate-in fade-in slide-in-from-top-1 duration-200">
          {error}
        </p>
      )}
    </div>
  )
}

export { PasswordField }
