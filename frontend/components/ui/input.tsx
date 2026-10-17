import * as React from "react"
import { cn } from "cn"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-12 w-full min-w-0 rounded-xl border border-white/[0.08] bg-white/[0.03] px-4 text-[0.95rem] shadow-[inset_0_1px_0_rgb(255_255_255/0.03)] transition-[border-color,background-color,box-shadow] duration-200 outline-none placeholder:text-muted-foreground/50",
        "hover:border-white/15 focus-visible:border-brand-blue/70 focus-visible:bg-white/[0.05] focus-visible:ring-4 focus-visible:ring-brand-blue/15",
        "disabled:cursor-not-allowed disabled:opacity-60",
        "aria-invalid:border-destructive/70 aria-invalid:ring-4 aria-invalid:ring-destructive/10",
        className
      )}
      {...props}
    />
  )
}

export { Input }
