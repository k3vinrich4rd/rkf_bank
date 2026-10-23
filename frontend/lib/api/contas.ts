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

/** Header que torna depósito, saque e transferência seguros contra repetição. */
export const HEADER_IDEMPOTENCIA = "Idempotency-Key";

export const contasApi = {
  /**
   * Contas do usuário logado: com token de CLIENTE, `GET /contas` já devolve só as dele (contrato 2.0, R13).
   * Lista simples, sem paginação: um cliente tem poucas contas.
   */
  async minhas() {
    const contas = await apiRequest<ContaResponse[]>("/contas");
    logContas.debug("Contas do usuário carregadas", { quantidade: contas.length });
    return contas;
  },

  /**
   * Abre conta no cadastro do usuário logado (contrato 2.6): a API tira o cliente do token, então não há
   * `clienteId` para mandar nem para errar. ADMIN sem cadastro de cliente → `422`.
   */
  async abrir(data: AbrirContaRequest) {
    logContas.info("Abrindo conta", { tipoConta: data.tipoContaEnum });
    const conta = await apiRequest<ContaResponse>("/contas", { method: "POST", body: data });
    logContas.info("Conta aberta", { contaId: conta.id, clienteId: conta.clienteId });
    return conta;
  },

  /** Desde o contrato 2.6, `page` ou `size` na URL já pedem a página (`paginado=true` é obsoleto). */
  async listar({ page = 0, size = 10 }: PageRequest = {}) {
    const pagina = await apiRequest<Page<ContaResponse>>("/contas", { query: { page, size } });
    logContas.debug("Contas listadas", { page, size, totalElements: pagina.totalElements });
    return pagina;
  },

  /**
   * Movimentações exigem `Idempotency-Key` (contrato 2.1): a mesma chave em uma repetição (rede caiu, clique duplo)
   * faz a API responder 204 sem movimentar de novo. Gere uma chave por operação e reuse-a só para repetir a mesma.
   */
  async depositar(contaId: Uuid, data: MovimentacaoRequest, chaveIdempotencia: string) {
    logContas.info("Depositando", { contaId, valor: data.valor });
    await apiRequest<void>(`/contas/${contaId}/depositos`, {
      method: "POST",
      body: data,
      headers: { [HEADER_IDEMPOTENCIA]: chaveIdempotencia },
    });
    logContas.info("Depósito concluído", { contaId });
  },

  async sacar(contaId: Uuid, data: MovimentacaoRequest, chaveIdempotencia: string) {
    logContas.info("Sacando", { contaId, valor: data.valor });
    await apiRequest<void>(`/contas/${contaId}/saques`, {
      method: "POST",
      body: data,
      headers: { [HEADER_IDEMPOTENCIA]: chaveIdempotencia },
    });
    logContas.info("Saque concluído", { contaId });
  },

  async transferir(data: TransferenciaRequest, chaveIdempotencia: string) {
    const contexto = { contaOrigemId: data.contaOrigemId, contaDestinoId: data.contaDestinoId };
    logContas.info("Transferindo", { ...contexto, valor: data.valor });
    await apiRequest<void>("/transferencias", {
      method: "POST",
      body: data,
      headers: { [HEADER_IDEMPOTENCIA]: chaveIdempotencia },
    });
    logContas.info("Transferência concluída", contexto);
  },

  async extrato(contaId: Uuid) {
    const lancamentos = await apiRequest<LancamentoResponse[]>(`/contas/${contaId}/lancamentos`);
    logContas.debug("Extrato carregado", { contaId, quantidade: lancamentos.length });
    return lancamentos;
  },
};
