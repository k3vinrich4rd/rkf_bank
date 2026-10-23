import { logClientes } from "@/lib/logger";
import { apiRequest } from "./client";
import type {
  AtualizarClienteRequest,
  CadastroClienteRequest,
  ClienteResponse,
  EncerramentoResponse,
  EncerrarCadastroRequest,
  Page,
  PageRequest,
} from "./types";

export const clientesApi = {
  async cadastrar(data: CadastroClienteRequest) {
    // CPF e e-mail são mascarados pelo logger.
    logClientes.info("Cadastrando cliente", { cpf: data.cpf, email: data.email });
    const cliente = await apiRequest<ClienteResponse>("/clientes", { method: "POST", body: data });
    logClientes.info("Cliente cadastrado", { clienteId: cliente.id, uf: cliente.endereco.uf });
    return cliente;
  },

  async listar({ page = 0, size = 10 }: PageRequest = {}) {
    const pagina = await apiRequest<Page<ClienteResponse>>("/clientes", {
      query: { paginado: true, page, size },
    });
    logClientes.debug("Clientes listados", { page, size, totalElements: pagina.totalElements });
    return pagina;
  },

  /**
   * Meus dados (LGPD, contrato 2.5): `me` é sempre o cliente do token, nunca um id na URL.
   * ADMIN sem cadastro de cliente → `422`.
   */
  async meusDados() {
    const cliente = await apiRequest<ClienteResponse>("/clientes/me");
    logClientes.debug("Meus dados carregados", { clienteId: cliente.id });
    return cliente;
  },

  /** Corrige só os campos enviados. Loga **quais** mudaram, nunca os valores. */
  async atualizarMeusDados(data: AtualizarClienteRequest) {
    logClientes.info("Atualizando meus dados", { campos: Object.keys(data) });
    const cliente = await apiRequest<ClienteResponse>("/clientes/me", { method: "PATCH", body: data });
    logClientes.info("Meus dados atualizados", { clienteId: cliente.id });
    return cliente;
  },

  /** Depois do `200`, todo token do titular dá `401`: quem chama precisa apagar a sessão. */
  async encerrarCadastro(data: EncerrarCadastroRequest) {
    // A senha é mascarada pelo logger.
    logClientes.info("Encerrando cadastro");
    const encerramento = await apiRequest<EncerramentoResponse>("/clientes/me/encerramento", {
      method: "POST",
      body: data,
    });
    logClientes.info("Cadastro encerrado", { anonimizacaoPrevistaPara: encerramento.anonimizacaoPrevistaPara });
    return encerramento;
  },
};
