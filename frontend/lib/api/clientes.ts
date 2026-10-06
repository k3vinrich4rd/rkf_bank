import { apiRequest } from "./client";
import type {
  CadastroClienteRequest,
  ClienteResponse,
  Page,
  PageRequest,
} from "./types";

export const clientesApi = {
  cadastrar: (data: CadastroClienteRequest) =>
    apiRequest<ClienteResponse>("/clientes", { method: "POST", body: data }),

  listar: ({ page = 0, size = 10 }: PageRequest = {}) =>
    apiRequest<Page<ClienteResponse>>("/clientes", {
      query: { paginado: true, page, size },
    }),
};
