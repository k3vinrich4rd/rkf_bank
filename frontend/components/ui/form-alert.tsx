import type { ReactNode } from "react"
import { CircleAlert } from "lucide-react"

interface FormAlertProps {
  children: ReactNode
}

/** Erro geral de um formulário (o que não pertence a um campo específico). */
function FormAlert({ children }: FormAlertProps) {
  return (
    <p
      role="alert"
      className="flex items-start gap-2.5 rounded-xl border border-destructive/25 bg-destructive/[0.07] px-4 py-3 text-sm text-destructive animate-in fade-in slide-in-from-top-1 duration-200"
    >
      <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {children}
    </p>
  )
}

export { FormAlert }
