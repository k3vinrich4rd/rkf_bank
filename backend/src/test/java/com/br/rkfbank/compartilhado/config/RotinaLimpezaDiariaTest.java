package com.br.rkfbank.compartilhado.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// A rotina nao conhece os modulos: chama cada LimpezaPeriodica com o mesmo instante.
class RotinaLimpezaDiariaTest {

    @Test
    void deveChamarTodasAsLimpezasComOMesmoInstante() {
        List<Instant> chamadas = new ArrayList<>();
        LimpezaPeriodica idempotencia = chamadas::add;
        LimpezaPeriodica tentativasDeLogin = chamadas::add;
        LimpezaPeriodica tokensDeRenovacao = chamadas::add;

        new RotinaLimpezaDiaria(List.of(idempotencia, tentativasDeLogin, tokensDeRenovacao)).executar();

        assertEquals(3, chamadas.size());
        assertSame(chamadas.get(0), chamadas.get(1));
        assertSame(chamadas.get(1), chamadas.get(2));
    }
}
