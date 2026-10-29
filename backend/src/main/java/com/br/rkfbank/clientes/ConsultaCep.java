package com.br.rkfbank.clientes;

// Porta de saida (arquitetura hexagonal): o que o modulo de clientes precisa do mundo de fora, dito na lingua dele.
// O cadastro e a correcao de endereco (titular) so conhecem esta interface; quem fala HTTP com o ViaCEP e o
// adaptador clientes.viacep.ViaCepConsultaCep. Trocar de provedor, ter um reserva ou um cache = adaptador novo,
// sem mexer na regra. Nos testes, um falso devolve o endereco na hora, sem rede.
public interface ConsultaCep {

    // cep: so os 8 digitos. Contrato de erro igual para qualquer adaptador:
    // CEP que nao existe -> RegraNegocioException(CEP_NAO_ENCONTRADO) (422);
    // provedor fora do ar, lento ou com erro -> ServicoExternoIndisponivelException (503).
    EnderecoDoCep buscar(String cep);
}
