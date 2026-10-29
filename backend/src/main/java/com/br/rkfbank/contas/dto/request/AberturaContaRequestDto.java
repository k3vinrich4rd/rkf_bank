package com.br.rkfbank.contas.dto.request;

import com.br.rkfbank.contas.TipoContaEnum;
import jakarta.validation.constraints.NotNull;

// Corpo do POST /contas. So o tipo: o dono vem do token, a agencia e sempre 0001 e o numero (com digito
// verificador) e gerado pelo banco. Numero de conta e identidade do banco, nao escolha de quem abre.
public record AberturaContaRequestDto(
    @NotNull(message = "tipoConta é obrigatório e deve ser CORRENTE ou POUPANCA")
    TipoContaEnum tipoConta
) {}
