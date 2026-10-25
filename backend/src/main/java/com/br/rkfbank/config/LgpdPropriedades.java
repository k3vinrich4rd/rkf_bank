package com.br.rkfbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Period;

// rkfbank.lgpd.*: por quanto tempo o cadastro encerrado fica guardado antes de ser anonimizado.
// Padrao 5 anos (Lei 9.613/1998, art. 10). Period aceita "5y", "60m", "1825d".
@ConfigurationProperties(prefix = "rkfbank.lgpd")
public record LgpdPropriedades(Period retencaoAposEncerramento) {

    public LgpdPropriedades {
        if (retencaoAposEncerramento == null) {
            retencaoAposEncerramento = Period.ofYears(5);
        }
    }
}
