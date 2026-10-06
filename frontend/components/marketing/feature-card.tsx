import type { Feature } from "@/config/marketing";
import { cn } from "@/lib/utils";

interface FeatureCardProps {
  feature: Feature;
  className?: string;
}

export function FeatureCard({ feature, className }: FeatureCardProps) {
  const { icon: Icon, title, description } = feature;

  return (
    <article
      className={cn(
        "group relative overflow-hidden rounded-2xl border border-border bg-surface/70 p-6 transition-all duration-300 hover:-translate-y-1 hover:border-brand-blue/40 hover:shadow-xl hover:shadow-brand-blue/10",
        className,
      )}
    >
      <div
        aria-hidden="true"
        className="absolute -top-16 -right-16 size-40 rounded-full bg-brand-blue/10 opacity-0 blur-3xl transition-opacity duration-300 group-hover:opacity-100"
      />
      <span className="relative grid size-11 place-items-center rounded-xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <h3 className="relative mt-5 text-lg font-semibold">{title}</h3>
      <p className="relative mt-2 text-sm leading-relaxed text-muted-foreground">
        {description}
      </p>
    </article>
  );
}
