package com.br.rkfbank.compartilhado.erros;

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

    // Contrato com o front: o tipo CodigoErro de frontend/lib/api/types.ts tem que listar exatamente estes codigos.
    // Divergiu? node scripts/sincronizar-codigos-erro.mjs
    @Test
    void frontendConheceTodosOsCodigos() throws java.io.IOException {
        String tipos = java.nio.file.Files.readString(java.nio.file.Path.of("frontend/lib/api/types.ts"));
        java.util.regex.Matcher bloco = java.util.regex.Pattern.compile("export type CodigoErro =([^;]*);").matcher(tipos);
        org.junit.jupiter.api.Assertions.assertTrue(bloco.find(), "tipo CodigoErro nao encontrado no frontend");
        java.util.Set<String> noFront = new java.util.TreeSet<>();
        java.util.regex.Matcher codigo = java.util.regex.Pattern.compile("\"([A-Z_]+)\"").matcher(bloco.group(1));
        while (codigo.find()) {
            noFront.add(codigo.group(1));
        }
        java.util.Set<String> noBack = new java.util.TreeSet<>();
        for (CodigoErroEnum c : CodigoErroEnum.values()) {
            noBack.add(c.name());
        }
        assertEquals(noBack, noFront, "rode: node scripts/sincronizar-codigos-erro.mjs");
    }

    @Test
    void excecaoLevaOCodigoEAMensagemDoCatalogo() {
        RegraNegocioException ex = new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE);
        assertEquals(CodigoErroEnum.SALDO_INSUFICIENTE, ex.getCodigo());
        assertEquals("Saldo insuficiente", ex.getMessage());
    }
}
