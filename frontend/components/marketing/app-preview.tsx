import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Bell,
  Eye,
  Landmark,
  Receipt,
  type LucideIcon,
} from "lucide-react";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { Cents } from "@/types";

interface QuickAction {
  icon: LucideIcon;
  label: string;
}

interface PreviewEntry {
  description: string;
  date: string;
  amount: Cents;
}

const quickActions: readonly QuickAction[] = [
  { icon: ArrowLeftRight, label: "Transferir" },
  { icon: Landmark, label: "Depositar" },
  { icon: ArrowUpRight, label: "Sacar" },
  { icon: Receipt, label: "Extrato" },
];

/** Dados ilustrativos — esta tela é uma vitrine, não consome a API. */
const previewEntries: readonly PreviewEntry[] = [
  { description: "Transferência recebida", date: "Hoje, 09:41", amount: 25000 },
  { description: "Saque", date: "Ontem, 18:12", amount: -8000 },
  { description: "Depósito", date: "03 out", amount: 150000 },
];

const PREVIEW_BALANCE: Cents = 1284590;

/** Mockup do app exibido no hero. */
export function AppPreview({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "w-full rounded-3xl border border-border bg-surface/90 p-5 shadow-2xl shadow-black/40 backdrop-blur",
        className,
      )}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-muted-foreground">Olá, Renan</p>
          <p className="text-sm font-semibold">Conta corrente</p>
        </div>
        <span className="grid size-9 place-items-center rounded-full bg-muted text-muted-foreground">
          <Bell className="size-4" />
        </span>
      </div>

      <div className="mt-5 rounded-2xl bg-gradient-brand p-px">
        <div className="rounded-[calc(1rem-1px)] bg-background/90 p-4">
          <p className="flex items-center gap-2 text-xs text-muted-foreground">
            Saldo disponível <Eye className="size-3.5" />
          </p>
          <p className="mt-1 text-2xl font-semibold tracking-tight tabular-nums">
            {formatCurrency(PREVIEW_BALANCE)}
          </p>
        </div>
      </div>

      <ul className="mt-5 grid grid-cols-4 gap-2">
        {quickActions.map(({ icon: Icon, label }) => (
          <li key={label} className="flex flex-col items-center gap-1.5">
            <span className="grid size-11 place-items-center rounded-xl border border-border bg-muted/60 text-brand-cyan">
              <Icon className="size-4.5" />
            </span>
            <span className="text-[0.65rem] text-muted-foreground">{label}</span>
          </li>
        ))}
      </ul>

      <div className="mt-6">
        <p className="text-xs font-medium text-muted-foreground">Últimos lançamentos</p>
        <ul className="mt-2 divide-y divide-border">
          {previewEntries.map((entry) => (
            <PreviewEntryRow key={entry.description} entry={entry} />
          ))}
        </ul>
      </div>
    </div>
  );
}

function PreviewEntryRow({ entry }: { entry: PreviewEntry }) {
  const isCredit = entry.amount > 0;
  const Icon = isCredit ? ArrowDownLeft : ArrowUpRight;

  return (
    <li className="flex items-center gap-3 py-2.5">
      <span
        className={cn(
          "grid size-8 place-items-center rounded-full",
          isCredit ? "bg-brand-cyan/10 text-brand-cyan" : "bg-muted text-muted-foreground",
        )}
      >
        <Icon className="size-4" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm">{entry.description}</p>
        <p className="text-[0.7rem] text-muted-foreground">{entry.date}</p>
      </div>
      <p
        className={cn(
          "text-sm font-medium tabular-nums",
          isCredit ? "text-brand-cyan" : "text-foreground",
        )}
      >
        {isCredit ? "+" : "−"} {formatCurrency(Math.abs(entry.amount))}
      </p>
    </li>
  );
}
