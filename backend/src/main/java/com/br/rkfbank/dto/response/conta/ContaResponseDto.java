package com.br.rkfbank.dto.response.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import java.math.BigDecimal;
import java.util.UUID;

// O que a API devolve de uma conta. Separado da entidade Conta: a coluna "versao" (lock otimista) e a lista
// de lancamentos ficam de fora, e a entidade pode mudar sem quebrar o contrato com o frontend.
public record ContaResponseDto(
    UUID id,
    UUID clienteId,
    String agencia,
    String numeroConta,
    TipoContaEnum tipoConta,
    BigDecimal saldo,
    boolean ativa
) {}
