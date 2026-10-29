package com.br.rkfbank.compartilhado.erros;

// Uma dependencia externa (hoje, o ViaCEP) falhou ou demorou demais -> 503 Service Unavailable.
// A culpa nao e de quem chamou: o front pode sugerir tentar de novo em instantes.
public class ServicoExternoIndisponivelException extends ErroApiException {
    public ServicoExternoIndisponivelException() {
        super(CodigoErroEnum.SERVICO_INDISPONIVEL);
    }

    // Guarda a causa original (timeout, erro de rede) para o log; a resposta leva so a mensagem do CodigoErroEnum.
    public ServicoExternoIndisponivelException(Throwable causa) {
        super(CodigoErroEnum.SERVICO_INDISPONIVEL, causa);
    }
}
