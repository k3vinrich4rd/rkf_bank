"use client"

import { useEffect, useState } from "react"
import { Check, Copy } from "lucide-react"
import { Button } from "@/components/ui/button"

const TEMPO_CONFIRMACAO_MS = 2000

interface CopyButtonProps {
  valor: string
  /** Texto do botão, ex.: "Copiar ID". */
  rotulo: string
}

/** Copia `valor` para a área de transferência e confirma por 2s. Falha em silêncio se o navegador bloquear. */
function CopyButton({ valor, rotulo }: CopyButtonProps) {
  const [copiado, setCopiado] = useState(false)

  useEffect(() => {
    if (!copiado) return
    const timer = setTimeout(() => setCopiado(false), TEMPO_CONFIRMACAO_MS)
    return () => clearTimeout(timer)
  }, [copiado])

  async function copiar() {
    try {
      await navigator.clipboard.writeText(valor)
      setCopiado(true)
    } catch {
      // Sem permissão de área de transferência: o valor continua visível para copiar à mão.
    }
  }

  return (
    <Button type="button" variant="outline" size="sm" onClick={copiar} aria-live="polite">
      {copiado ? <Check data-icon="inline-start" aria-hidden="true" /> : <Copy data-icon="inline-start" aria-hidden="true" />}
      {copiado ? "Copiado" : rotulo}
    </Button>
  )
}

export { CopyButton }
