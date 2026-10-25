package com.br.rkfbank.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.repositories.ContaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

// Concorrencia de verdade: varias transferencias da MESMA conta ao mesmo tempo, cada uma na sua transacao, no MySQL.
// O H2 dos outros testes nao serve para isso (o isolamento e o lock de linha sao do MySQL/InnoDB).
class ConcorrenciaMySqlTest extends MySqlIntegracaoBase {

    private static final int THREADS = 8;

    @Autowired
    private ContaRepository contaRepository;

    // Cada thread tenta tirar 10,00 de uma conta com 50,00. Seja qual for a ordem, o dinheiro tem que fechar:
    // sucessos (204) nunca passam do saldo, quem perde a corrida recebe 409 CONCORRENCIA ou 422 SALDO_INSUFICIENTE,
    // e saldo + extrato batem centavo por centavo. Sem o @Version, duas threads leriam 50,00 e as duas gravariam 40,00.
    @Test
    void transferenciasSimultaneasNaoCriamNemSomemDinheiro() throws Exception {
        String token = clienteNovoComToken();
        String origem = abrirConta(token, "CORRENTE");
        String destino = abrirConta(token, "POUPANCA");
        depositar(token, origem, "50.00");

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            respostas.add(pool.submit(() -> {
                largada.await();
                return mockMvc.perform(post("/transferencias").header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contaOrigemId\":\"" + origem + "\",\"contaDestinoId\":\"" + destino + "\",\"valor\":10.00}"))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();
        int sucessos = 0;
        for (Future<Integer> resposta : respostas) {
            int status = resposta.get();
            assertTrue(status == 204 || status == 409 || status == 422, "status inesperado: " + status);
            if (status == 204) {
                sucessos++;
            }
        }
        pool.shutdown();

        assertTrue(sucessos >= 1 && sucessos <= 5, "sucessos: " + sucessos);
        assertEquals(0, new BigDecimal("50.00").subtract(BigDecimal.TEN.multiply(BigDecimal.valueOf(sucessos)))
                .compareTo(contaRepository.findById(UUID.fromString(origem)).orElseThrow().getSaldo()));
        assertEquals(0, BigDecimal.TEN.multiply(BigDecimal.valueOf(sucessos))
                .compareTo(contaRepository.findById(UUID.fromString(destino)).orElseThrow().getSaldo()));
        String extrato = mockMvc.perform(get("/contas/" + destino + "/lancamentos").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        assertEquals(sucessos, jsonMapper.readTree(extrato).size());
    }

    // O lock otimista, de forma deterministica: duas copias da mesma conta; a segunda a gravar perde (409 na API).
    @Test
    void copiaDesatualizadaDaContaPerdeParaOLockOtimista() throws Exception {
        String token = clienteNovoComToken();
        UUID id = UUID.fromString(abrirConta(token, "CORRENTE"));

        Conta primeira = contaRepository.findById(id).orElseThrow();
        Conta segunda = contaRepository.findById(id).orElseThrow();
        primeira.setSaldo(new BigDecimal("1.00"));
        contaRepository.saveAndFlush(primeira);

        segunda.setSaldo(new BigDecimal("999.00"));
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> contaRepository.saveAndFlush(segunda));
        assertEquals(0, new BigDecimal("1.00").compareTo(contaRepository.findById(id).orElseThrow().getSaldo()));
    }
}
