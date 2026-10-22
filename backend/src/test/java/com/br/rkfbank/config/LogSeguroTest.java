package com.br.rkfbank.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Injecao de log (CWE-117): nada que vem da requisicao pode criar uma linha nova no log.
class LogSeguroTest {

    @Test
    void deveTrocarQuebraDeLinhaParaNaoForjarUmaLinhaNovaNoLog() {
        String ataque = "/contas\r\n2026-10-22 INFO Login concluido para o admin";

        String limpo = LogSeguro.texto(ataque);

        assertFalse(limpo.contains("\r"));
        assertFalse(limpo.contains("\n"));
        assertEquals("/contas__2026-10-22 INFO Login concluido para o admin", limpo);
    }

    @Test
    void deveTrocarOutrosCaracteresDeControle() {
        assertEquals("a_b_c", LogSeguro.texto("a\tb\u0007c"));
    }

    @Test
    void deveCortarValorGiganteParaNaoEncherOLog() {
        String limpo = LogSeguro.texto("x".repeat(5_000));

        assertEquals(LogSeguro.TAMANHO_MAXIMO + 3, limpo.length());
        assertTrue(limpo.endsWith("..."));
    }

    @Test
    void deveManterTextoNormalENullLegivel() {
        assertEquals("/contas/123/saque", LogSeguro.texto("/contas/123/saque"));
        assertEquals("null", LogSeguro.texto(null));
    }
}
