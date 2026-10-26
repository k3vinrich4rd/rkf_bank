package com.br.rkfbank.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// A versao vigente dos Termos de Uso e da Politica de Privacidade (LGPD). O texto mora no frontend (/termos e
// /privacidade); quando ele muda, sobe esta versao (LGPD_TERMOS_VERSAO) e o cadastro passa a exigir o aceite da nova.
// Classe propria (e nao uma String solta) para os testes conseguirem trocar a versao com um mock.
@Component
public class TermosDeUso {

    private final String versaoVigente;

    public TermosDeUso(@Value("${rkfbank.lgpd.termos-versao:2026-10-26}") String versaoVigente) {
        this.versaoVigente = versaoVigente;
    }

    public String versaoVigente() {
        return versaoVigente;
    }
}
