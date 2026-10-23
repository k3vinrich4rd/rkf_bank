package com.br.rkfbank.dto.request.conta;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import jakarta.validation.constraints.NotNull;

// Corpo do POST /contas. So o tipo: o dono vem do token, a agencia e sempre 0001 e o numero (com digito
// verificador) e gerado pelo banco. Numero de conta e identidade do banco, nao escolha de quem abre.
public record AberturaContaRequestDto(
    @NotNull(message = "tipoConta e obrigatorio e deve ser CORRENTE ou POUPANCA")
    TipoContaEnum tipoConta
) {}
