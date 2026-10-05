package com.br.rkfbank.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String cpf = value.replaceAll("\\D", "");
        if (cpf.length() != 11) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int digito1 = calcularDigito(cpf.substring(0, 9), 10);
        int digito2 = calcularDigito(cpf.substring(0, 9) + digito1, 11);

        return cpf.equals(cpf.substring(0, 9) + digito1 + digito2);
    }

    private int calcularDigito(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * (pesoInicial - i);
        }

        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}

