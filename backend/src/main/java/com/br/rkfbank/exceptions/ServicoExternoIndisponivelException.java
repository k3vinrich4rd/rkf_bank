package com.br.rkfbank.exceptions;

// Uma dependencia externa (hoje, o ViaCEP) falhou ou demorou demais -> 503 Service Unavailable.
// A culpa nao e de quem chamou: o front pode sugerir tentar de novo em instantes.
public class ServicoExternoIndisponivelException extends RuntimeException {
    public ServicoExternoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    // Guarda a causa original (timeout, erro de rede) para o log; a mensagem que sai na resposta continua generica.
    public ServicoExternoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
