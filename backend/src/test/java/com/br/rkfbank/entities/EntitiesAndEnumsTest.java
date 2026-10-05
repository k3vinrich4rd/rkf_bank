package com.br.rkfbank.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.br.rkfbank.entities.enums.TipoContaEnum;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.entities.converters.TipoLancamentoEnumConverter;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntitiesAndEnumsTest {

    @Test
    void deveCobrirGettersESettersDeEnderecoClienteEConta() {
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");
        endereco.setComplemento("Apto 45");

        Cliente cliente = new Cliente();
        cliente.setNomeCompleto("Joao Silva");
        cliente.setCpf("52998224725");
        cliente.setEmail("joao@rkfbank.com");
        cliente.setTelefone("11999999999");
        cliente.setEndereco(endereco);

        Conta conta = new Conta();
        conta.setNumeroConta("12345678");
        conta.setAgencia("0001");
        conta.setTipoConta(TipoContaEnum.CORRENTE);
        conta.setSaldo(new BigDecimal("50.00"));
        conta.setAtiva(true);
        conta.setCliente(cliente);

        assertEquals("01001-000", endereco.getCep());
        assertEquals("Praca da Se", endereco.getLogradouro());
        assertEquals("Se", endereco.getBairro());
        assertEquals("Sao Paulo", endereco.getCidade());
        assertEquals("SP", endereco.getUf());
        assertEquals("3550308", endereco.getIbge());
        assertEquals("123", endereco.getNumero());
        assertEquals("Apto 45", endereco.getComplemento());
        assertNull(endereco.getId());

        assertEquals("Joao Silva", cliente.getNomeCompleto());
        assertEquals("52998224725", cliente.getCpf());
        assertEquals("joao@rkfbank.com", cliente.getEmail());
        assertEquals("11999999999", cliente.getTelefone());
        assertEquals(endereco, cliente.getEndereco());
        assertNotNull(cliente.getContas());
        assertTrue(cliente.getContas().isEmpty());

        assertEquals("12345678", conta.getNumeroConta());
        assertEquals("0001", conta.getAgencia());
        assertEquals(TipoContaEnum.CORRENTE, conta.getTipoConta());
        assertEquals(new BigDecimal("50.00"), conta.getSaldo());
        assertTrue(conta.isAtiva());
        assertEquals(cliente, conta.getCliente());
    }

    @Test
    void deveCobrirLancamentoEAoSalvar() {
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();

        Lancamento lancamento = new Lancamento();
        lancamento.setTipoLancamentoEnum(TipoLancamentoEnum.TRANSFERENCIA);
        lancamento.setValor(new BigDecimal("10.00"));
        lancamento.setContaOrigemId(origem);
        lancamento.setContaDestinoId(destino);
        lancamento.setDescricao("Transferencia teste");
        lancamento.aoSalvar();

        assertEquals(TipoLancamentoEnum.TRANSFERENCIA, lancamento.getTipoLancamentoEnum());
        assertEquals(new BigDecimal("10.00"), lancamento.getValor());
        assertEquals(origem, lancamento.getContaOrigemId());
        assertEquals(destino, lancamento.getContaDestinoId());
        assertEquals("Transferencia teste", lancamento.getDescricao());
        assertNotNull(lancamento.getDataHora());
    }

    @Test
    void deveCobrirDescricoesDosEnums() {
        assertEquals("Conta Corrente", TipoContaEnum.CORRENTE.getDescricao());
        assertEquals("Conta Poupança", TipoContaEnum.POUPANCA.getDescricao());

        assertEquals("Depósito", TipoLancamentoEnum.DEPOSITO.getDescricao());
        assertEquals("Saque", TipoLancamentoEnum.SAQUE.getDescricao());
        assertEquals("Transferência", TipoLancamentoEnum.TRANSFERENCIA.getDescricao());
    }

    @Test
    void deveConverterValoresLegadosDeLancamento() {
        TipoLancamentoEnumConverter converter = new TipoLancamentoEnumConverter();

        assertEquals(TipoLancamentoEnum.DEPOSITO, converter.convertToEntityAttribute("CREDITO"));
        assertEquals(TipoLancamentoEnum.SAQUE, converter.convertToEntityAttribute("DEBITO"));
        assertEquals("DEPOSITO", converter.convertToDatabaseColumn(TipoLancamentoEnum.DEPOSITO));
    }
}
