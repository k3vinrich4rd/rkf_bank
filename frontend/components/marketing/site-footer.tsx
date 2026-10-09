import Link from "next/link";
import { Logo } from "@/components/brand/logo";
import { mainNav, siteConfig } from "@/config/site";
import { ROUTES } from "@/lib/routes";
import { Container } from "./container";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-surface/40">
      <Container className="flex flex-col gap-10 py-12 md:flex-row md:items-start md:justify-between">
        <div>
          <Link href={ROUTES.home} aria-label={`${siteConfig.fullName} — início`}>
            <Logo />
          </Link>
          <p className="mt-4 text-xs tracking-[0.3em] text-muted-foreground uppercase">
            {siteConfig.tagline}
          </p>
        </div>

        <nav aria-label="Rodapé">
          <ul className="grid grid-cols-2 gap-x-12 gap-y-3 text-sm sm:grid-cols-3">
            {mainNav.map((item) => (
              <li key={item.href}>
                <a href={item.href} className="text-muted-foreground transition-colors hover:text-foreground">
                  {item.label}
                </a>
              </li>
            ))}
            <li>
              <Link href={ROUTES.login} className="text-muted-foreground transition-colors hover:text-foreground">
                Entrar
              </Link>
            </li>
          </ul>
        </nav>
      </Container>

      <Container className="flex flex-col gap-2 border-t border-border py-6 text-xs text-muted-foreground sm:flex-row sm:justify-between">
        <p>
          © {year} {siteConfig.fullName}. Todos os direitos reservados.
        </p>
        <p>Projeto em desenvolvimento — dados exibidos são ilustrativos.</p>
      </Container>
    </footer>
  );
}
