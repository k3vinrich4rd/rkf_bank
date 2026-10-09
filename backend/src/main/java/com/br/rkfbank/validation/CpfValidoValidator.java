package com.br.rkfbank.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

//Classe que implementa a validação do CPF
//Responsável por verificar se o CPF é válido conforme as regras de validação do CPF brasileiro.
public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {


    //Metodo que realiza a validação do CPF
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        // Remove caracteres não numéricos do CPF
        String cpf = value.replaceAll("\\D", "");
        if (cpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (ex: 111.111.111-11)
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int digito1 = calcularDigito(cpf.substring(0, 9), 10);
        int digito2 = calcularDigito(cpf.substring(0, 9) + digito1, 11);

        return cpf.equals(cpf.substring(0, 9) + digito1 + digito2);
    }

    //Metodo auxiliar para calcular o dígito verificador do CPF
    private int calcularDigito(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * (pesoInicial - i);
        }

        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}

