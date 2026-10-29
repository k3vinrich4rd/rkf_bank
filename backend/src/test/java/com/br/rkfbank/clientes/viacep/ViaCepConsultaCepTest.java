package com.br.rkfbank.clientes.viacep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.rkfbank.clientes.ConsultaCep;
import com.br.rkfbank.clientes.Endereco;
import com.br.rkfbank.clientes.EnderecoDoCep;
import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import com.br.rkfbank.compartilhado.erros.RegraNegocioException;
import com.br.rkfbank.compartilhado.erros.ServicoExternoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Adaptador da porta ConsultaCep: traduz o ViaCEP para o formato do RKF Bank e mantem o contrato de erro
// (422 para CEP inexistente, 503 para o ViaCEP fora) que o cadastro e a correcao do titular esperam.
class ViaCepConsultaCepTest {

    private ViaCepClient viaCepClient;
    private ConsultaCep consultaCep;

    @BeforeEach
    void setUp() {
        viaCepClient = mock(ViaCepClient.class);
        consultaCep = new ViaCepConsultaCep(viaCepClient);
    }

    @Test
    void deveTraduzirORetornoDoViaCepParaOEnderecoDoBanco() {
        when(viaCepClient.buscarPorCep("06020010")).thenReturn(new ViaCepResponseDto(
                "06020-010", "Rua Antonio Agu", "Centro", "Osasco", "SP", "3534401", false));

        EnderecoDoCep endereco = consultaCep.buscar("06020010");

        assertEquals(new EnderecoDoCep("06020-010", "Rua Antonio Agu", "Centro", "Osasco", "SP", "3534401"), endereco);
    }

    @ParameterizedTest(name = "CEP {0} do ViaCEP vira {1}")
    @CsvSource({"06020010, 06020-010", "06020-010, 06020-010", "0602001, 0602001"})
    void deveGuardarOCepComHifen(String doViaCep, String esperado) {
        when(viaCepClient.buscarPorCep("06020010")).thenReturn(new ViaCepResponseDto(
                doViaCep, "Rua", "Centro", "Osasco", "SP", "3534401", false));

        assertEquals(esperado, consultaCep.buscar("06020010").cep());
    }

    @Test
    void deveManterCepNuloSemQuebrar() {
        assertNull(ViaCepConsultaCep.comHifen(null));
    }

    @Test
    void deveRepassarO422DeCepInexistente() {
        RegraNegocioException erro = new RegraNegocioException(CodigoErroEnum.CEP_NAO_ENCONTRADO);
        when(viaCepClient.buscarPorCep("99999999")).thenThrow(erro);

        assertSame(erro, assertThrows(RegraNegocioException.class, () -> consultaCep.buscar("99999999")));
    }

    @Test
    void deveRepassarO503DoViaCepForaDoAr() {
        when(viaCepClient.buscarPorCep("06020010")).thenThrow(new ServicoExternoIndisponivelException());

        assertThrows(ServicoExternoIndisponivelException.class, () -> consultaCep.buscar("06020010"));
    }

    @Test
    void deveCopiarOEnderecoOficialSemMexerEmNumeroEComplemento() {
        Endereco endereco = new Endereco();
        endereco.setNumero("123");
        endereco.setComplemento("Apto 45");

        new EnderecoDoCep("06020-010", "Rua Antonio Agu", "Centro", "Osasco", "SP", "3534401").aplicarEm(endereco);

        assertEquals("06020-010", endereco.getCep());
        assertEquals("Osasco", endereco.getCidade());
        assertEquals("3534401", endereco.getIbge());
        assertEquals("123", endereco.getNumero());
        assertEquals("Apto 45", endereco.getComplemento());
    }
}
