"use client";

import Link from "next/link";
import { ArrowRight, Check, LoaderCircle, MapPin, RotateCw, ShieldCheck, TriangleAlert, UserRound } from "lucide-react";
import { ContaShell } from "@/components/conta/conta-shell";
import { ContaVoltar } from "@/components/conta/conta-voltar";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/ui/form-alert";
import { FormField } from "@/components/ui/form-field";
import type { ClienteResponse, EncerramentoResponse } from "@/lib/api";
import { ROUTES } from "@/lib/routes";
import { useEncerrarCadastro, type EncerrarCadastroState } from "./use-encerrar-cadastro";
import { useMeusDados, type MeusDadosState } from "./use-meus-dados";

/** Meus dados (LGPD): ver, corrigir e encerrar o próprio cadastro. O estado vive nos dois hooks. */
export function MeusDadosScreen() {
  const dados = useMeusDados();
  const encerrar = useEncerrarCadastro();

  return (
    <ContaShell>
      {encerrar.encerramento ? (
        <CadastroEncerrado encerramento={encerrar.encerramento} />
      ) : (
        <div className="flex flex-col animate-in fade-in slide-in-from-bottom-2 duration-500">
          <ContaVoltar />
          <span className="mt-8 grid size-12 place-items-center rounded-2xl border border-brand-blue/30 bg-brand-blue/10 text-brand-cyan">
            <UserRound className="size-5" aria-hidden="true" />
          </span>
          <h1 className="mt-6 text-3xl font-semibold tracking-tight">Meus dados</h1>
          <p className="mt-3 mb-8 text-muted-foreground">
            O que o RKF guarda sobre você. Corrija o que mudou ou encerre o cadastro quando quiser.
          </p>

          {dados.etapa === "carregando" && <MeusDadosCarregando />}
          {dados.etapa === "erro" && (
            <div className="flex flex-col gap-4">
              <FormAlert>{dados.erroBusca}</FormAlert>
              <Button type="button" variant="outline" size="xl" onClick={dados.tentarDeNovo}>
                <RotateCw data-icon="inline-start" aria-hidden="true" />
                Tentar de novo
              </Button>
            </div>
          )}
          {dados.etapa === "pronta" && dados.cliente && (
            <>
              <FormularioDados dados={dados} cliente={dados.cliente} />
              <ZonaEncerramento encerrar={encerrar} />
            </>
          )}
        </div>
      )}
    </ContaShell>
  );
}

function FormularioDados({ dados, cliente }: { dados: MeusDadosState; cliente: ClienteResponse }) {
  const { campo, alterados, desfazer, erroGeral, aviso, enviando, handleSubmit } = dados;
  const { endereco } = cliente;

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
      <div className="flex items-center justify-between rounded-2xl border border-white/[0.08] bg-white/[0.02] px-4 py-3.5">
        <div>
          <p className="text-[0.8rem] font-medium text-foreground/80">CPF</p>
          <p className="mt-0.5 font-mono text-sm tracking-wide">{mascararCpf(cliente.cpf)}</p>
        </div>
        <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <ShieldCheck className="size-3.5 text-brand-cyan" aria-hidden="true" />
          É o seu login: não muda
        </span>
      </div>

      <FormField {...campo("nomeCompleto")} label="Nome completo" autoComplete="name" maxLength={120} />
      <FormField {...campo("email")} label="E-mail" type="email" autoComplete="email" inputMode="email" />
      <FormField {...campo("telefone")} label="Celular" type="tel" autoComplete="tel-national" inputMode="numeric" />

      <div className="grid grid-cols-[1fr_6.5rem] gap-4">
        <FormField {...campo("cep")} label="CEP" autoComplete="postal-code" inputMode="numeric" hint="Busca o endereço" />
        <FormField {...campo("numero")} label="Número" autoComplete="address-line2" maxLength={20} />
      </div>
      <p className="-mt-2 flex items-start gap-2 text-xs text-muted-foreground">
        <MapPin className="mt-0.5 size-3.5 shrink-0 text-brand-cyan" aria-hidden="true" />
        {endereco.logradouro}, {endereco.bairro} · {endereco.cidade} – {endereco.uf}
      </p>
      <FormField {...campo("complemento")} label="Complemento" hint="Opcional" autoComplete="address-line3" maxLength={80} />

      <div className="mt-3 flex flex-col gap-4">
        {erroGeral && <FormAlert>{erroGeral}</FormAlert>}
        {aviso && (
          <p role="status" className="flex items-center gap-2.5 rounded-xl border border-emerald-500/25 bg-emerald-500/[0.07] px-4 py-3 text-sm text-emerald-400 animate-in fade-in duration-200">
            <Check className="size-4 shrink-0" aria-hidden="true" />
            {aviso}
          </p>
        )}

        <Button type="submit" variant="brand" size="xl" disabled={enviando || alterados === 0} className="h-[3.25rem] w-full">
          {enviando ? (
            <>
              <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
              Salvando…
            </>
          ) : alterados === 0 ? (
            "Nada para salvar"
          ) : (
            `Salvar ${alterados === 1 ? "1 alteração" : `${alterados} alterações`}`
          )}
        </Button>
        {alterados > 0 && !enviando && (
          <button type="button" onClick={desfazer} className="self-center rounded-lg px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground">
            Desfazer alterações
          </button>
        )}
      </div>
    </form>
  );
}

function ZonaEncerramento({ encerrar }: { encerrar: EncerrarCadastroState }) {
  const { confirmando, abrirConfirmacao, cancelar, senha, erroGeral, enviando, handleSubmit } = encerrar;

  return (
    <section aria-labelledby="encerrar-titulo" className="mt-14 rounded-2xl border border-destructive/25 bg-destructive/[0.04] p-5">
      <h2 id="encerrar-titulo" className="flex items-center gap-2 font-medium">
        <TriangleAlert className="size-4 text-destructive" aria-hidden="true" />
        Encerrar cadastro
      </h2>
      <p className="mt-2 text-sm text-muted-foreground">
        Suas contas ficam inativas e o acesso acaba na hora. Por lei, os dados ficam guardados por 5 anos e depois são
        anonimizados. Precisa estar com saldo zero.
      </p>

      {!confirmando ? (
        <Button type="button" variant="outline" size="lg" onClick={abrirConfirmacao} className="mt-5 border-destructive/40 text-destructive hover:bg-destructive/10">
          Quero encerrar meu cadastro
        </Button>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="mt-5 flex flex-col gap-4 animate-in fade-in slide-in-from-top-1 duration-200">
          <FormField {...senha} label="Confirme com a sua senha" type="password" autoComplete="current-password" autoFocus />
          {erroGeral && <FormAlert>{erroGeral}</FormAlert>}
          <div className="flex flex-col-reverse gap-3 sm:flex-row">
            <Button type="button" variant="ghost" size="lg" onClick={cancelar} disabled={enviando} className="sm:flex-1">
              Cancelar
            </Button>
            <Button type="submit" variant="destructive" size="lg" disabled={enviando} className="sm:flex-1">
              {enviando ? (
                <>
                  <LoaderCircle className="animate-spin" data-icon="inline-start" aria-hidden="true" />
                  Encerrando…
                </>
              ) : (
                "Encerrar de vez"
              )}
            </Button>
          </div>
        </form>
      )}
    </section>
  );
}

function CadastroEncerrado({ encerramento }: { encerramento: EncerramentoResponse }) {
  return (
    <div role="status" className="animate-in fade-in slide-in-from-bottom-3 duration-500">
      <span className="grid size-12 place-items-center rounded-2xl border border-white/10 bg-white/[0.04] text-muted-foreground">
        <Check className="size-5" aria-hidden="true" />
      </span>
      <h1 className="mt-8 text-3xl font-semibold tracking-tight">Cadastro encerrado</h1>
      <p className="mt-3 text-muted-foreground">
        Suas contas estão inativas e o acesso foi encerrado em todos os aparelhos. Obrigado por ter usado o RKF.
      </p>

      <dl className="mt-8 divide-y divide-white/[0.06] rounded-2xl border border-white/[0.08] bg-white/[0.02] text-sm">
        <div className="flex justify-between gap-4 px-5 py-4">
          <dt className="text-muted-foreground">Encerrado em</dt>
          <dd>{DATA_HORA.format(new Date(encerramento.encerradoEm))}</dd>
        </div>
        <div className="flex justify-between gap-4 px-5 py-4">
          <dt className="text-muted-foreground">Dados anonimizados em</dt>
          <dd>{dataSemFuso(encerramento.anonimizacaoPrevistaPara)}</dd>
        </div>
      </dl>
      <p className="mt-4 text-xs leading-relaxed text-muted-foreground">
        Até lá, a lei de prevenção à lavagem de dinheiro obriga o banco a guardar o cadastro e as operações.
      </p>

      <Button asChild variant="brand" size="xl" className="mt-10 h-[3.25rem] w-full">
        <Link href={ROUTES.home}>
          Voltar ao início
          <ArrowRight data-icon="inline-end" aria-hidden="true" />
        </Link>
      </Button>
    </div>
  );
}

function MeusDadosCarregando() {
  return (
    <div role="status" aria-label="Carregando seus dados" className="flex flex-col gap-5">
      {Array.from({ length: 5 }, (_, i) => (
        <div key={i} className="h-[4.5rem] animate-pulse rounded-2xl bg-white/[0.04]" />
      ))}
    </div>
  );
}

const DATA_HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short", timeZone: "America/Sao_Paulo" });

/** "2031-10-22" → "22/10/2031", sem passar por `Date` (que leria como UTC e poderia voltar um dia). */
function dataSemFuso(data: string) {
  const [ano, mes, dia] = data.split("-");
  return `${dia}/${mes}/${ano}`;
}

/** Mesma máscara dos logs: só os 5 últimos dígitos aparecem. */
function mascararCpf(cpf: string) {
  const digitos = cpf.replace(/\D/g, "");
  return digitos.length === 11 ? `***.***.${digitos.slice(6, 9)}-${digitos.slice(9)}` : "***";
}
