package com.br.rkfbank.compartilhado.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

// Rotinas agendadas (@Scheduled) e a trava entre instancias (ShedLock).
//
// Sem a trava, com duas instancias da API no ar, as duas acordariam no mesmo horario e fariam o mesmo trabalho.
// Com ela, antes de rodar, cada instancia tenta pegar a linha da rotina na tb_shedlock (V11): so quem conseguir
// executa. Se a instancia cair no meio, a trava expira sozinha (lockAtMostFor) e a proxima execucao segue.
// defaultLockAtMostFor e o teto de seguranca; cada rotina diz o seu no @SchedulerLock.
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ConfiguracaoRotinas {

    @Bean
    public LockProvider travaDasRotinas(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("tb_shedlock")
                // Hora do banco, e nao a de cada maquina: instancias com relogios diferentes ainda concordam.
                .usingDbTime()
                .build());
    }
}
