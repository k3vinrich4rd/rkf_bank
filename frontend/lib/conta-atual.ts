import type { Uuid } from "@/lib/api";

/**
 * "Minha conta" guardada no navegador.
 *
 * Enquanto não existe login (ver "Acesso à conta sem login" em
 * planing/decisoes.md), é assim que o usuário volta para a conta dele:
 * o cadastro grava o `clienteId` e a abertura de conta grava o `contaId`.
 *
 * O `localStorage` pode estar bloqueado (aba anônima, dados limpos), por
 * isso toda leitura e escrita é protegida e falha em silêncio.
 */
export interface ContaAtual {
  clienteId: Uuid;
  /** Preenchido depois que a conta é aberta. */
  contaId?: Uuid;
}

const CHAVE = "rkf:conta-atual";
/** O evento `storage` só dispara em outras abas; este avisa a aba atual. */
const EVENTO = "rkf:conta-atual-mudou";

function lerBruto(): string | null {
  try {
    return window.localStorage.getItem(CHAVE);
  } catch {
    return null;
  }
}

function escreverBruto(valor: string | null) {
  try {
    if (valor === null) window.localStorage.removeItem(CHAVE);
    else window.localStorage.setItem(CHAVE, valor);
  } catch {
    // Sem armazenamento disponível: segue sem lembrar a conta.
  }
  window.dispatchEvent(new Event(EVENTO));
}

function converter(bruto: string | null): ContaAtual | null {
  if (!bruto) return null;
  try {
    const valor: unknown = JSON.parse(bruto);
    if (typeof valor === "object" && valor !== null && "clienteId" in valor && typeof valor.clienteId === "string") {
      return valor as ContaAtual;
    }
  } catch {
    // Conteúdo corrompido: trata como vazio.
  }
  return null;
}

// Cache pelo texto bruto: o `useSyncExternalStore` exige o mesmo objeto
// enquanto o valor não muda, senão a tela re-renderiza sem parar.
let cacheBruto: string | null = null;
let cacheValor: ContaAtual | null = null;

export function lerContaAtual(): ContaAtual | null {
  const bruto = lerBruto();
  if (bruto !== cacheBruto) {
    cacheBruto = bruto;
    cacheValor = converter(bruto);
  }
  return cacheValor;
}

export function salvarContaAtual(conta: ContaAtual | null) {
  escreverBruto(conta === null ? null : JSON.stringify(conta));
}

/** Avisa quando a conta atual muda, nesta aba ou em outra. */
export function observarContaAtual(aoMudar: () => void): () => void {
  const aoMudarStorage = (event: StorageEvent) => {
    if (event.key === null || event.key === CHAVE) aoMudar();
  };
  window.addEventListener(EVENTO, aoMudar);
  window.addEventListener("storage", aoMudarStorage);
  return () => {
    window.removeEventListener(EVENTO, aoMudar);
    window.removeEventListener("storage", aoMudarStorage);
  };
}
