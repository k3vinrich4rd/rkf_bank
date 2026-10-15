import { logContas } from "@/lib/logger";
import { ApiError, apiRequest } from "./client";
import type {
  AbrirContaRequest,
  ContaResponse,
  LancamentoResponse,
  MovimentacaoRequest,
  Page,
  PageRequest,
  TransferenciaRequest,
  Uuid,
} from "./types";

/** Maior página aceita pelo backend (`Paginacao.MAX_PAGE_SIZE`). */
const TAMANHO_MAXIMO_PAGINA = 100;

// TODO(backend): apagar quando existir `GET /contas/{contaId}` (contrato, seção 7) — aí `buscar`
// vira uma chamada só. Até lá, procura a conta página a página na listagem.
async function procurarNaListagem(contaId: Uuid): Promise<ContaResponse> {
  for (let page = 0; ; page++) {
    const pagina = await apiRequest<Page<ContaResponse>>("/contas", {
      query: { paginado: true, page, size: TAMANHO_MAXIMO_PAGINA },
    });
    const conta = pagina.content.find(({ id }) => id === contaId);
    if (conta) return conta;
    if (page >= pagina.totalPages - 1) throw new ApiError(404, "Conta não encontrada");
  }
}

export const contasApi = {
  /** Uma conta pelo id. Mesmo formato da abertura (contrato 4.1). */
  async buscar(contaId: Uuid) {
    const conta = await procurarNaListagem(contaId);
    logContas.debug("Conta carregada", { contaId });
    return conta;
  },

  async abrir(clienteId: Uuid, data: AbrirContaRequest) {
    logContas.info("Abrindo conta", { clienteId, tipoConta: data.tipoContaEnum });
    const conta = await apiRequest<ContaResponse>(`/contas/clientes/${clienteId}`, {
      method: "POST",
      body: data,
    });
    logContas.info("Conta aberta", { contaId: conta.id, clienteId });
    return conta;
  },

  async listar({ page = 0, size = 10 }: PageRequest = {}) {
    const pagina = await apiRequest<Page<ContaResponse>>("/contas", {
      query: { paginado: true, page, size },
    });
    logContas.debug("Contas listadas", { page, size, totalElements: pagina.totalElements });
    return pagina;
  },

  async depositar(contaId: Uuid, data: MovimentacaoRequest) {
    logContas.info("Depositando", { contaId, valor: data.valor });
    await apiRequest<void>(`/contas/${contaId}/deposito`, { method: "POST", body: data });
    logContas.info("Depósito concluído", { contaId });
  },

  async sacar(contaId: Uuid, data: MovimentacaoRequest) {
    logContas.info("Sacando", { contaId, valor: data.valor });
    await apiRequest<void>(`/contas/${contaId}/saque`, { method: "POST", body: data });
    logContas.info("Saque concluído", { contaId });
  },

  async transferir(data: TransferenciaRequest) {
    const contexto = { contaOrigemId: data.contaOrigemId, contaDestinoId: data.contaDestinoId };
    logContas.info("Transferindo", { ...contexto, valor: data.valor });
    await apiRequest<void>("/contas/transferencia", { method: "POST", body: data });
    logContas.info("Transferência concluída", contexto);
  },

  async extrato(contaId: Uuid) {
    const lancamentos = await apiRequest<LancamentoResponse[]>(`/contas/${contaId}/lancamentos`);
    logContas.debug("Extrato carregado", { contaId, quantidade: lancamentos.length });
    return lancamentos;
  },
};
