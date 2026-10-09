package com.br.rkfbank.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class CpfValidoValidatorTest {

    private final CpfValidoValidator validator = new CpfValidoValidator();

    @Test
    void deveAceitarCpfValido() {
        assertTrue(validator.isValid("52998224725", null));
    }

    @Test
    void deveAceitarCpfNuloOuEmBranco() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    void deveAceitarCpfFormatadoComPontuacao() {
        assertTrue(validator.isValid("529.982.247-25", null));
    }

    @Test
    void deveRejeitarCpfComDigitosRepetidos() {
        assertFalse(validator.isValid("11111111111", null));
    }

    @Test
    void deveRejeitarCpfComDigitoInvalido() {
        assertFalse(validator.isValid("52998224724", null));
    }

    @Test
    void deveRejeitarCpfCurto() {
        assertFalse(validator.isValid("123", null));
    }

    @Test
    void deveRejeitarCpfComCaracterNaoNumerico() {
        assertFalse(validator.isValid("5299822472A", null));
    }

    @Test
    void deveCobrirRamoDeRestoDezNoCalculoDoDigito() throws Exception {
        Method metodo = CpfValidoValidator.class.getDeclaredMethod("calcularDigito", String.class, int.class);
        metodo.setAccessible(true);

        int digito = (int) metodo.invoke(validator, "000000006", 10);

        assertTrue(digito == 0);
    }
}
