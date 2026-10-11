import * as React from "react"
import { cn } from "cn"
import { Input } from "@/components/ui/input"

interface FormFieldProps extends React.ComponentProps<typeof Input> {
  name: string
  label: string
  /** Texto discreto ao lado do label, ex.: "Opcional". */
  hint?: string
  error?: string
}

/** Label + input + mensagem de erro, já ligados por `id` e `aria-*`. */
function FormField({ name, label, hint, error, className, ...props }: FormFieldProps) {
  const errorId = `${name}-error`

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <label htmlFor={name} className="flex items-baseline justify-between text-[0.8rem] font-medium text-foreground/80">
        {label}
        {hint && <span className="text-xs font-normal text-muted-foreground/70">{hint}</span>}
      </label>

      <Input
        id={name}
        name={name}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...props}
      />

      {error && (
        <p id={errorId} className="text-xs text-destructive animate-in fade-in slide-in-from-top-1 duration-200">
          {error}
        </p>
      )}
    </div>
  )
}

export { FormField }
