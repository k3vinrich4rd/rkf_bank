package com.br.rkfbank.exceptions;

// Catalogo de erros da API: cada constante e o "codigo" estavel do ErroResponse (contrato 3.0) e carrega a mensagem
// que vai no "message". Um lugar so para o texto de cada erro: o service diz QUAL erro aconteceu, nunca escreve a frase.
//
// Quem consome a API decide pelo status e, quando o mesmo status tem causas diferentes (409 de idempotencia x 409 de
// concorrencia, 422 de senha errada x 422 de saldo), pelo codigo. Nunca pelo texto, que pode mudar.
// Codigo novo pode entrar a qualquer momento; renomear ou apagar um existente quebra o frontend.
//
// Fica de fora de proposito: a mensagem de cada CAMPO invalido (Bean Validation nos DTOs e RequisicaoInvalidaException),
// que descreve aquele campo e vai em "campos"; o erro em si sai como CAMPOS_INVALIDOS.
public enum CodigoErroEnum {

    // 400: pedido mal formado
    CAMPOS_INVALIDOS("Requisição inválida"),
    JSON_INVALIDO("JSON inválido ou tipo de campo incompatível"),
    PARAMETRO_INVALIDO("Parâmetro de URL inválido"),

    // 401: sem identidade valida
    AUTENTICACAO_NECESSARIA("Autenticação necessária: envie o token no header Authorization (Bearer)"),
    TOKEN_INVALIDO("Token inválido ou expirado; faça login novamente"),
    ACESSO_ALTERADO("Seu acesso foi alterado ou desativado; faça login novamente"),
    CREDENCIAIS_INVALIDAS("CPF ou senha inválidos"),

    // 403: identidade valida, sem permissao
    PERFIL_SEM_PERMISSAO("Acesso negado para o seu perfil"),
    CONTA_DE_OUTRO_CLIENTE("Conta não pertence ao usuário logado"),

    // 404: recurso inexistente
    CLIENTE_NAO_ENCONTRADO("Cliente não encontrado"),
    CONTA_NAO_ENCONTRADA("Conta não encontrada"),
    USUARIO_NAO_ENCONTRADO("Usuário não encontrado"),

    // 409: colide com o estado atual do banco
    CPF_JA_CADASTRADO("CPF já cadastrado"),
    EMAIL_JA_CADASTRADO("E-mail já cadastrado"),
    CADASTRO_DUPLICADO("CPF ou e-mail já cadastrado"),
    NUMERO_CONTA_INDISPONIVEL("Não foi possível gerar um número de conta livre; tente novamente"),
    CONCORRENCIA("A conta foi alterada por outra operação; tente novamente"),
    IDEMPOTENCIA_EM_ANDAMENTO("Esta operação já está sendo processada; repita com a mesma Idempotency-Key para ver o resultado"),

    // 422: regra de negocio recusou
    CEP_NAO_ENCONTRADO("CEP não encontrado"),
    CONTA_INATIVA("Conta inativa"),
    VALOR_INVALIDO("Valor deve ser maior que zero"),
    SALDO_INSUFICIENTE("Saldo insuficiente"),
    MESMA_CONTA("Conta de origem e destino não podem ser iguais"),
    SENHA_ATUAL_INCORRETA("Senha atual incorreta"),
    SENHA_REPETIDA("A nova senha precisa ser diferente da atual"),
    SENHA_INCORRETA("Senha incorreta"),
    SEM_CADASTRO_CLIENTE("Seu usuário não tem cadastro de cliente"),
    SALDO_NAO_ZERADO("Transfira o saldo das suas contas antes de encerrar o cadastro"),
    PERFIL_CLIENTE_SEM_CADASTRO("Usuário sem cadastro de cliente não pode virar CLIENTE; desative o acesso"),
    CADASTRO_ENCERRADO("O titular encerrou o cadastro; o acesso não pode ser reativado"),
    PROPRIA_SENHA_PELA_GESTAO("Para trocar a sua própria senha use PATCH /usuarios/me/senha"),
    PROPRIO_ACESSO("Você não pode alterar o seu próprio perfil ou acesso; peça a outro ADMIN"),
    IDEMPOTENCIA_CHAVE_REUSADA("Idempotency-Key já usada em outra requisição; gere uma chave nova para esta operação"),

    // 429 e 503
    MUITAS_TENTATIVAS("Muitas tentativas de login; aguarde alguns minutos e tente de novo"),
    SERVICO_INDISPONIVEL("ViaCEP indisponível; tente novamente em instantes");

    private final String mensagem;

    CodigoErroEnum(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
