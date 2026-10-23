import { logContas } from "@/lib/logger";
import { authApi } from "./auth";
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
   * Contas do usuário logado. Com token de CLIENTE, `GET /contas` já devolve só as dele (contrato 2.0, R13), mas
   * com token de ADMIN devolve **todas**: por isso o filtro pelo `clienteId` da sessão. ADMIN sem cadastro de
   * cliente (`clienteId` nulo) não tem conta própria → lista vazia.
   * Lista simples, sem paginação: um cliente tem poucas contas.
   */
  async minhas() {
    const [contas, { clienteId }] = await Promise.all([
      apiRequest<ContaResponse[]>("/contas"),
      authApi.sessao(),
    ]);
    const minhas = contas.filter((conta) => conta.clienteId === clienteId);
    logContas.debug("Contas do usuário carregadas", { quantidade: minhas.length });
    return minhas;
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
