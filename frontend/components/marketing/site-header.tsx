import Link from "next/link";
import { Logo } from "@/components/brand/logo";
import { Button } from "@/components/ui/button";
import { mainNav, siteConfig } from "@/config/site";
import { ROUTES } from "@/lib/routes";
import { LoginButton } from "./login-button";
import { MobileNav } from "./mobile-nav";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/70 backdrop-blur-xl">
      <div className="relative mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-6 px-4 sm:px-6 lg:px-8">
        <Link href={ROUTES.home} aria-label={`${siteConfig.fullName} — início`}>
          <Logo />
        </Link>

        <nav aria-label="Navegação principal" className="hidden md:block">
          <ul className="flex items-center gap-1">
            {mainNav.map((item) => (
              <li key={item.href}>
                <a
                  href={item.href}
                  className="rounded-lg px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          <Button
            asChild
            variant="brand"
            size="lg"
            className="hidden rounded-full px-5 md:inline-flex"
          >
            <Link href={ROUTES.signUp}>Abrir conta</Link>
          </Button>
          <LoginButton />
          <MobileNav items={mainNav} />
        </div>
      </div>
    </header>
  );
}
