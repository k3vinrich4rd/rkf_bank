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
  abrir: (clienteId: Uuid, data: AbrirContaRequest) =>
    apiRequest<ContaResponse>(`/contas/clientes/${clienteId}`, {
      method: "POST",
      body: data,
    }),

  listar: ({ page = 0, size = 10 }: PageRequest = {}) =>
    apiRequest<Page<ContaResponse>>("/contas", {
      query: { paginado: true, page, size },
    }),

  depositar: (contaId: Uuid, data: MovimentacaoRequest) =>
    apiRequest<void>(`/contas/${contaId}/deposito`, { method: "POST", body: data }),

  sacar: (contaId: Uuid, data: MovimentacaoRequest) =>
    apiRequest<void>(`/contas/${contaId}/saque`, { method: "POST", body: data }),

  transferir: (data: TransferenciaRequest) =>
    apiRequest<void>("/contas/transferencia", { method: "POST", body: data }),

  extrato: (contaId: Uuid) =>
    apiRequest<LancamentoResponse[]>(`/contas/${contaId}/lancamentos`),
};
