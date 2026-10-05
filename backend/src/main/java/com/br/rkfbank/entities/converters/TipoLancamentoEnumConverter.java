package com.br.rkfbank.entities.converters;

import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoLancamentoEnumConverter implements AttributeConverter<TipoLancamentoEnum, String> {

    @Override
    public String convertToDatabaseColumn(TipoLancamentoEnum tipo) {
        return tipo == null ? null : tipo.name();
    }

    @Override
    public TipoLancamentoEnum convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return switch (valor) {
            case "CREDITO" -> TipoLancamentoEnum.DEPOSITO;
            case "DEBITO" -> TipoLancamentoEnum.SAQUE;
            default -> TipoLancamentoEnum.valueOf(valor);
        };
    }
}
