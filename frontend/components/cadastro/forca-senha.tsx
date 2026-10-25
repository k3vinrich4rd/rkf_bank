/**
 * Medidor de força da senha: só orienta, não bloqueia (a regra de verdade é a do contrato: 8 a 72 caracteres).
 * Conta critérios simples e visíveis, para a pessoa entender o que melhora a senha.
 */
const NIVEIS = [
  { rotulo: "Muito curta", cor: "bg-destructive" },
  { rotulo: "Fraca", cor: "bg-destructive" },
  { rotulo: "Razoável", cor: "bg-amber-400" },
  { rotulo: "Boa", cor: "bg-brand-blue" },
  { rotulo: "Forte", cor: "bg-emerald-400" },
] as const;

function pontuar(senha: string): number {
  if (senha.length < 8) return 0;
  let pontos = 1;
  if (senha.length >= 12) pontos++;
  if (/[a-z]/i.test(senha) && /\d/.test(senha)) pontos++;
  if (/[A-Z]/.test(senha) && /[a-z]/.test(senha) && /[^a-z\d]/i.test(senha)) pontos++;
  return pontos;
}

export function ForcaSenha({ senha }: { senha: string }) {
  if (!senha) return null;
  const pontos = pontuar(senha);
  const nivel = NIVEIS[pontos] ?? NIVEIS[0];

  return (
    <div className="flex items-center gap-3 animate-in fade-in duration-200" aria-live="polite">
      <div className="grid flex-1 grid-cols-4 gap-1.5" aria-hidden="true">
        {[1, 2, 3, 4].map((barra) => (
          <span
            key={barra}
            className={`h-1 rounded-full transition-colors duration-300 ${barra <= pontos ? nivel.cor : "bg-white/[0.08]"}`}
          />
        ))}
      </div>
      <span className="w-20 text-right font-mono text-[0.7rem] text-muted-foreground">{nivel.rotulo}</span>
    </div>
  );
}
