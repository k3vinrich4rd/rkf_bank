package com.br.rkfbank.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

// O catalogo e o unico lugar com o texto de cada erro: nenhum pode ficar sem mensagem, e duas causas diferentes
// com a mesma frase confundiriam quem le (o front decide pelo codigo, mas a pessoa le a mensagem).
class CodigoErroEnumTest {

    @Test
    void todoCodigoTemMensagem() {
        for (CodigoErroEnum codigo : CodigoErroEnum.values()) {
            assertFalse(codigo.getMensagem().isBlank(), codigo.name());
        }
    }

    @Test
    void mensagensNaoSeRepetem() {
        long distintas = Arrays.stream(CodigoErroEnum.values()).map(CodigoErroEnum::getMensagem).distinct().count();
        assertEquals(CodigoErroEnum.values().length, distintas);
    }

    @Test
    void excecaoLevaOCodigoEAMensagemDoCatalogo() {
        RegraNegocioException ex = new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE);
        assertEquals(CodigoErroEnum.SALDO_INSUFICIENTE, ex.getCodigo());
        assertEquals("Saldo insuficiente", ex.getMessage());
    }
}
