import { logClientes } from "@/lib/logger";
import { apiRequest } from "./client";
import type {
  CadastroClienteRequest,
  ClienteResponse,
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
};
