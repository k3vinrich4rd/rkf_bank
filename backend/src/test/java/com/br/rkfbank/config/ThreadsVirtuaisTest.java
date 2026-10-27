package com.br.rkfbank.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;

// spring.threads.virtual.enabled=true: o executor da aplicacao (o mesmo interruptor liga o Tomcat, o @Scheduled e
// o @Async) tem que rodar as tarefas em thread virtual. Se alguem desligar a propriedade, este teste avisa.
@SpringBootTest
@ActiveProfiles("test")
class ThreadsVirtuaisTest {

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private TaskExecutor executor;

    @Test
    void tarefasDaAplicacaoRodamEmThreadVirtual() throws Exception {
        CompletableFuture<Boolean> virtual = new CompletableFuture<>();

        executor.execute(() -> virtual.complete(Thread.currentThread().isVirtual()));

        assertThat(virtual.get(5, TimeUnit.SECONDS)).isTrue();
    }
}
