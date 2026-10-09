/**
 * Tipos espelhando o contrato oficial da API
 * (`backend/README_CONTRATO_API.md`). Se o contrato mudar, atualize aqui
 * primeiro — o TypeScript aponta todos os pontos afetados.
 */

/** UUID trafegado como texto. */
export type Uuid = string;

/** Data/hora ISO-8601 em UTC, ex.: "2026-10-05T15:32:14Z". */
export type IsoDateTime = string;

/**
 * Valor monetário em reais com 2 casas, como a API envia hoje
 * (`BigDecimal` serializado como número JSON, ex.: 150.00).
 *
 * ⚠️ Decisão pendente em planing/decisoes.md ("Dinheiro"). Enquanto não
 * for resolvida, converta para centavos com `decimalToCents` antes de
 * fazer contas no frontend.
 */
export type DecimalAmount = number;

export type TipoConta = "CORRENTE" | "POUPANCA";
export type TipoLancamento = "DEPOSITO" | "SAQUE" | "TRANSFERENCIA";

// ── Clientes ────────────────────────────────────────────────────────────

export interface CadastroClienteRequest {
  nomeCompleto: string;
  /** 11 dígitos, sem máscara. */
  cpf: string;
  email: string;
  /** 10 ou 11 dígitos, sem máscara. */
  telefone: string;
  /** 8 dígitos, sem máscara. */
  cep: string;
  numero: string;
  complemento?: string;
}

export interface EnderecoResponse {
  cep: string;
  logradouro: string;
  bairro: string;
  cidade: string;
  uf: string;
  ibge: string;
  numero: string;
  complemento: string | null;
}

export interface ClienteResponse {
  id: Uuid;
  nomeCompleto: string;
  cpf: string;
  email: string;
  telefone: string;
  endereco: EnderecoResponse;
}

// ── Contas e movimentações ──────────────────────────────────────────────

export interface AbrirContaRequest {
  /** 4 dígitos. */
  agencia: string;
  /** 5 a 12 dígitos. */
  numeroConta: string;
  tipoContaEnum: TipoConta;
}

export interface ContaResponse {
  id: Uuid;
  clienteId: Uuid;
  agencia: string;
  numeroConta: string;
  tipoContaEnum: TipoConta;
  saldo: DecimalAmount;
  ativa: boolean;
}

export interface MovimentacaoRequest {
  valor: DecimalAmount;
  /** Até 140 caracteres. */
  descricao?: string;
}

export interface TransferenciaRequest extends MovimentacaoRequest {
  contaOrigemId: Uuid;
  contaDestinoId: Uuid;
}

export interface LancamentoResponse {
  id: Uuid;
  tipoLancamento: TipoLancamento;
  valor: DecimalAmount;
  contaOrigemId: Uuid | null;
  contaDestinoId: Uuid | null;
  descricao: string | null;
  dataHora: IsoDateTime;
}

// ── Paginação e erros ───────────────────────────────────────────────────

export interface PageRequest {
  page?: number;
  /** Entre 1 e 100. */
  size?: number;
}

/**
 * Formato de `Page<T>` do Spring Data. Atenção: serializar `PageImpl`
 * diretamente não é estável entre versões — ver revisão do contrato.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export interface ErroResponse {
  timestamp: IsoDateTime;
  status: number;
  error: string;
  message: string;
  path: string;
  campos: ErroCampo[];
}
