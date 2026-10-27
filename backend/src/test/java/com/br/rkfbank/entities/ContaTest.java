package com.br.rkfbank.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// Regras de saldo da Conta escritas ANTES do codigo (TDD): primeiro estes testes falharam (vermelho), depois a
// Conta ganhou depositar/sacar/transferirPara (verde) e por fim o ContaService passou a usar esses metodos (refatorar).
// Teste de unidade puro: sem Spring, sem banco, sem mock. A regra do dinheiro mora na entidade.
class ContaTest {

    private static Conta contaComSaldo(String saldo) {
        Conta conta = new Conta();
        conta.setSaldo(new BigDecimal(saldo));
        conta.setAtiva(true);
        return conta;
    }

    @Nested
    class Depositar {

        @Test
        void deveSomarOValorAoSaldo() {
            Conta conta = contaComSaldo("10.00");

            conta.depositar(new BigDecimal("150.00"));

            assertThat(conta.getSaldo()).isEqualByComparingTo("160.00");
        }

        @ParameterizedTest(name = "valor {0} é recusado")
        @ValueSource(strings = {"0.00", "-1.00"})
        void deveRecusarValorQueNaoEPositivo(String valor) {
            Conta conta = contaComSaldo("10.00");

            assertThatThrownBy(() -> conta.depositar(new BigDecimal(valor)))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoErroEnum.VALOR_INVALIDO));
            assertThat(conta.getSaldo()).isEqualByComparingTo("10.00");
        }
    }

    @Nested
    class Sacar {

        // A borda que mais gera bug: sacar EXATAMENTE o saldo tem que ser permitido.
        @ParameterizedTest(name = "saldo 100.00, saque {0} → saldo {1}")
        @CsvSource({"99.99, 0.01", "100.00, 0.00"})
        void deveSubtrairQuandoHaSaldo(String valor, String saldoFinal) {
            Conta conta = contaComSaldo("100.00");

            conta.sacar(new BigDecimal(valor));

            assertThat(conta.getSaldo()).isEqualByComparingTo(saldoFinal);
        }

        @Test
        void deveRecusarSaqueMaiorQueOSaldoSemMexerNoSaldo() {
            Conta conta = contaComSaldo("100.00");

            assertThatThrownBy(() -> conta.sacar(new BigDecimal("100.01")))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoErroEnum.SALDO_INSUFICIENTE));
            assertThat(conta.getSaldo()).isEqualByComparingTo("100.00");
        }

        @Test
        void deveRecusarValorQueNaoEPositivo() {
            Conta conta = contaComSaldo("100.00");

            assertThatThrownBy(() -> conta.sacar(BigDecimal.ZERO))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoErroEnum.VALOR_INVALIDO));
        }
    }

    @Nested
    class TransferirPara {

        @Test
        void deveDebitarAOrigemECreditarODestino() {
            Conta origem = contaComSaldo("100.00");
            Conta destino = contaComSaldo("5.00");

            origem.transferirPara(destino, new BigDecimal("25.00"));

            assertThat(origem.getSaldo()).isEqualByComparingTo("75.00");
            assertThat(destino.getSaldo()).isEqualByComparingTo("30.00");
        }

        @Test
        void naoDeveMexerEmNenhumaContaQuandoFaltaSaldo() {
            Conta origem = contaComSaldo("10.00");
            Conta destino = contaComSaldo("5.00");

            assertThatThrownBy(() -> origem.transferirPara(destino, new BigDecimal("10.01")))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoErroEnum.SALDO_INSUFICIENTE));
            assertThat(origem.getSaldo()).isEqualByComparingTo("10.00");
            assertThat(destino.getSaldo()).isEqualByComparingTo("5.00");
        }

        @Test
        void naoDeveTransferirParaAPropriaConta() {
            Conta conta = contaComSaldo("100.00");

            assertThatThrownBy(() -> conta.transferirPara(conta, new BigDecimal("1.00")))
                    .isInstanceOfSatisfying(RegraNegocioException.class,
                            e -> assertThat(e.getCodigo()).isEqualTo(CodigoErroEnum.MESMA_CONTA));
            assertThat(conta.getSaldo()).isEqualByComparingTo("100.00");
        }
    }
}
