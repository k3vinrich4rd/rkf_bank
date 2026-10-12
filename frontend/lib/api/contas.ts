import { logContas } from "@/lib/logger";
import { apiRequest } from "./client";
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

export const contasApi = {
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
