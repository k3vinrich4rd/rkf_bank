package com.br.rkfbank.clientes;

// Endereco oficial de um CEP, ja no formato do RKF Bank (cep com hifen, "cidade" em vez do "localidade" do ViaCEP).
// Numero e complemento nao vem do CEP: sao da pessoa, e o service completa.
public record EnderecoDoCep(String cep, String logradouro, String bairro, String cidade, String uf, String ibge) {

    // Copia os dados oficiais para o endereco do cliente (cadastro e correcao do titular usam o mesmo).
    public void aplicarEm(Endereco endereco) {
        endereco.setCep(cep);
        endereco.setLogradouro(logradouro);
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setUf(uf);
        endereco.setIbge(ibge);
    }
}
