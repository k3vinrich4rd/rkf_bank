package com.br.rkfbank.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CpfValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
// Anotacao de validacao propria (Bean Validation): @CpfValido num campo faz o Spring chamar o CpfValidoValidator
// junto com @NotBlank, @Email etc. quando o controller recebe @Valid. Erro vira 400 com o campo "cpf".
// message/groups/payload sao obrigatorios pela especificacao, mesmo sem uso aqui.
public @interface CpfValido {

    String message() default "cpf inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

