package com.br.rkfbank.clientes.viacep;

import com.br.rkfbank.clientes.ConsultaCep;
import com.br.rkfbank.clientes.EnderecoDoCep;
import org.springframework.stereotype.Component;

// Adaptador de saida: implementa a porta ConsultaCep com o ViaCEP. Aqui fica tudo o que e "do ViaCEP": o cliente
// HTTP (ViaCepClient, com timeout e 422/503), os nomes dele ("localidade") e o CEP que as vezes volta sem hifen.
@Component
public class ViaCepConsultaCep implements ConsultaCep {

    private final ViaCepClient viaCepClient;

    public ViaCepConsultaCep(ViaCepClient viaCepClient) {
        this.viaCepClient = viaCepClient;
    }

    @Override
    public EnderecoDoCep buscar(String cep) {
        ViaCepResponseDto viaCep = viaCepClient.buscarPorCep(cep);
        return new EnderecoDoCep(comHifen(viaCep.cep()), viaCep.logradouro(), viaCep.bairro(),
                viaCep.localidade(), viaCep.uf(), viaCep.ibge());
    }

    // O banco guarda o CEP no padrao do README ("06020-010"); o ViaCEP costuma mandar assim, mas nao e garantido.
    static String comHifen(String cep) {
        return cep != null && cep.matches("\\d{8}") ? cep.replaceAll("(\\d{5})(\\d{3})", "$1-$2") : cep;
    }
}
