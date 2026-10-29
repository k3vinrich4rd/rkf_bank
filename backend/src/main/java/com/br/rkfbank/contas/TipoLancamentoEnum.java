package com.br.rkfbank.contas;

// Tipo de cada linha do extrato. O front decide entrada x saida pelo contaDestinoId, nao pelo tipo.
public enum TipoLancamentoEnum {

    DEPOSITO("Depósito"),
    SAQUE("Saque"),
    // Ate a V13 havia tambem CREDITO e DEBITO, reservados e nunca usados: sairam. Tipo novo (tarifa, rendimento)
    // entra aqui junto com uma migracao que o acrescente ao CHECK da coluna.
    TRANSFERENCIA("Transferência");

    private final String descricao;

    TipoLancamentoEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
