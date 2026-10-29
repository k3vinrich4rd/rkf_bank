package com.br.rkfbank.compartilhado.config;

// Prepara um valor vindo de fora (URL, header, mensagem montada a partir da requisicao) para entrar no log.
//
// O problema que resolve: injecao de log (CWE-117). Se alguem manda uma URL com quebra de linha codificada, um valor
// gravado sem tratamento pode "inventar" uma linha nova no log, por exemplo uma linha falsa de "login concluido".
// Quem investiga um incidente passa a confiar numa mentira. A defesa e simples: o que veio de fora nunca carrega
// quebra de linha nem caractere de controle para o log.
//
// Abordagem: uma funcao so, usada em todo log.* que recebe dado da requisicao, em vez de cada classe tratar do seu
// jeito. O log4j2-spring.xml ainda codifica CR/LF da mensagem inteira (%enc{%m}{CRLF}) como segunda barreira.
public final class LogSeguro {

    // Teto do que entra no log: uma URL gigante nao enche o disco nem esconde as linhas seguintes.
    static final int TAMANHO_MAXIMO = 200;

    private LogSeguro() {
    }

    // Troca CR, LF e os demais caracteres de controle por "_" e corta no tamanho maximo. null vira "null".
    public static String texto(Object valor) {
        if (valor == null) {
            return "null";
        }
        String limpo = String.valueOf(valor)
                .replace('\r', '_')
                .replace('\n', '_')
                .replaceAll("\\p{Cntrl}", "_");
        return limpo.length() > TAMANHO_MAXIMO ? limpo.substring(0, TAMANHO_MAXIMO) + "..." : limpo;
    }
}
