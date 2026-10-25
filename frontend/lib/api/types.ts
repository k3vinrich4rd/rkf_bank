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
  /** 8 a 72 caracteres (e no máximo 72 bytes). Nunca volta na resposta. */
  senha: string;
  /** LGPD (contrato 3.1): sem `true`, a API responde `400` no campo `aceiteTermos`. */
  aceiteTermos: boolean;
  /** Versão dos termos que a pessoa viu (`TERMOS_VERSAO`); diferente da vigente → `422 TERMOS_DESATUALIZADOS`. */
  termosVersao?: string;
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
  /** Já vem **mascarado** da API (`***.982.247-**`, contrato 3.1): exiba como está. */
  cpf: string;
  email: string;
  telefone: string;
  endereco: EnderecoResponse;
  /** Versão dos termos aceita no cadastro; `null` em cadastros anteriores ao aceite (contrato 3.1). */
  termosVersao: string | null;
  termosAceitosEm: IsoDateTime | null;
}

/** `GET /termos` (público): a versão dos termos que o cadastro espera. */
export interface TermosResponse {
  versao: string;
}

/**
 * `PATCH /clientes/me` (LGPD, contrato 2.5): o titular corrige os próprios dados. Tudo opcional; mande só o
 * que mudou (corpo vazio → 400). CPF não muda. CEP novo exige `numero` (o endereço vem do ViaCEP).
 * `complemento: ""` apaga o complemento. Resposta: `ClienteResponse` atualizado.
 */
export interface AtualizarClienteRequest {
  /** 1 a 120 caracteres. */
  nomeCompleto?: string;
  /** Salvo em minúsculas; e-mail de outro cliente → 409. */
  email?: string;
  /** DDD + 8 ou 9 dígitos; pontuação é ignorada. */
  telefone?: string;
  /** 8 dígitos, com ou sem hífen. */
  cep?: string;
  numero?: string;
  complemento?: string;
}

/**
 * `POST /clientes/me/encerramento` (LGPD, contrato 2.5). Exige saldo zero em todas as contas (422).
 * Depois do 200, todo token do titular dá 401: apague a sessão como na troca de senha.
 */
export interface EncerrarCadastroRequest {
  senha: string;
}

export interface EncerramentoResponse {
  encerradoEm: IsoDateTime;
  /** Data (AAAA-MM-DD) a partir da qual os dados pessoais são anonimizados (5 anos depois). */
  anonimizacaoPrevistaPara: string;
  motivoDaRetencao: string;
}

// ── Autenticação ────────────────────────────────────────────────────────

export type Perfil = "CLIENTE" | "ADMIN";

export interface LoginRequest {
  /** Com ou sem máscara: o backend remove o que não é dígito. */
  cpf: string;
  senha: string;
}

/** Resposta de `POST /auth/login` no backend. Só o servidor do Next a vê: o token vira cookie `httpOnly`. */
export interface LoginResponse {
  token: string;
  tipo: "Bearer";
  expiraEmSegundos: number;
  /** `null` para o ADMIN, que não é cliente. */
  clienteId: Uuid | null;
  perfil: Perfil;
  /** Senha temporária (redefinida pelo ADMIN): só a troca de senha funciona até trocar (contrato 3.1). */
  deveTrocarSenha: boolean;
  /** Renova a sessão em `POST /auth/refresh` sem pedir a senha. Uso único; fica num cookie `httpOnly`. */
  refreshToken: string;
  refreshExpiraEmSegundos: number;
}

/**
 * Sessão como o navegador enxerga: **não vem do backend**, e sim das rotas `/api/auth/*` do próprio Next
 * (`app/api/auth`). É a resposta do login sem o token.
 */
export interface Sessao {
  clienteId: Uuid | null;
  perfil: Perfil;
  /** Quando o token de acesso vence; o `proxy.ts` renova antes, com o token de renovação. */
  expiraEm: IsoDateTime;
  /** Senha temporária: a tela leva direto para a troca de senha. */
  deveTrocarSenha: boolean;
}

// ── Contas e movimentações ──────────────────────────────────────────────

/** Só o tipo (contrato 3.0): o dono vem do token, a agência é 0001 e o número é gerado pelo banco. */
export interface AbrirContaRequest {
  tipoConta: TipoConta;
}

export interface ContaResponse {
  id: Uuid;
  clienteId: Uuid;
  agencia: string;
  numeroConta: string;
  tipoConta: TipoConta;
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
 * Página do contrato: DTO próprio do backend (`PaginaResponse`), sempre com estes 7 campos, estável entre versões
 * do Spring. Basta mandar `page` ou `size` para vir paginado; sem eles, a rota devolve a lista inteira.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** Gestão de usuários (só ADMIN, contrato 2.2). Sem tela por enquanto: tipos para quando houver. */
export interface UsuarioAdminResponse {
  /** Id de tb_usuario (não é o clienteId). */
  id: Uuid;
  /** Mascarado: "***.456.789-**". */
  cpf: string;
  /** null para o ADMIN que não é cliente. */
  nome: string | null;
  perfil: Perfil;
  ativo: boolean;
  clienteId: Uuid | null;
}

export interface AlterarPerfilRequest {
  perfil: Perfil;
}

export interface AlterarAcessoRequest {
  ativo: boolean;
}

/** `PATCH /usuarios/me/senha` (contrato 2.6). Depois do 204, o token atual deixa de valer: mande para o login. */
export interface TrocarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

/** `PATCH /admin/usuarios/{id}/senha`, só ADMIN. */
export interface RedefinirSenhaRequest {
  novaSenha: string;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}

/**
 * Código estável de cada erro (contrato 3.0), espelho do `CodigoErroEnum` do backend. Decida pelo `status` e,
 * quando o mesmo status tem causas diferentes (409 de idempotência × concorrência, 422 de senha × saldo), pelo
 * código. Nunca pelo texto do `message`, que é só para exibir.
 */
export type CodigoErro =
  | "CAMPOS_INVALIDOS"
  | "JSON_INVALIDO"
  | "PARAMETRO_INVALIDO"
  | "AUTENTICACAO_NECESSARIA"
  | "TOKEN_INVALIDO"
  | "ACESSO_ALTERADO"
  | "CREDENCIAIS_INVALIDAS"
  | "SESSAO_EXPIRADA"
  | "PERFIL_SEM_PERMISSAO"
  | "TROCA_DE_SENHA_OBRIGATORIA"
  | "CONTA_DE_OUTRO_CLIENTE"
  | "CLIENTE_NAO_ENCONTRADO"
  | "CONTA_NAO_ENCONTRADA"
  | "USUARIO_NAO_ENCONTRADO"
  | "CPF_JA_CADASTRADO"
  | "EMAIL_JA_CADASTRADO"
  | "CADASTRO_DUPLICADO"
  | "NUMERO_CONTA_INDISPONIVEL"
  | "CONCORRENCIA"
  | "IDEMPOTENCIA_EM_ANDAMENTO"
  | "CEP_NAO_ENCONTRADO"
  | "CONTA_INATIVA"
  | "VALOR_INVALIDO"
  | "SALDO_INSUFICIENTE"
  | "MESMA_CONTA"
  | "SENHA_ATUAL_INCORRETA"
  | "SENHA_REPETIDA"
  | "SENHA_INCORRETA"
  | "SEM_CADASTRO_CLIENTE"
  | "SALDO_NAO_ZERADO"
  | "PERFIL_CLIENTE_SEM_CADASTRO"
  | "CADASTRO_ENCERRADO"
  | "PROPRIA_SENHA_PELA_GESTAO"
  | "PROPRIO_ACESSO"
  | "TERMOS_DESATUALIZADOS"
  | "IDEMPOTENCIA_CHAVE_REUSADA"
  | "MUITAS_TENTATIVAS"
  | "SERVICO_INDISPONIVEL";

export interface ErroResponse {
  timestamp: IsoDateTime;
  status: number;
  error: string;
  codigo: CodigoErro;
  message: string;
  path: string;
  campos: ErroCampo[];
}
