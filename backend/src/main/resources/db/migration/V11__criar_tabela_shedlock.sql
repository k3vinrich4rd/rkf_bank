-- Trava das rotinas agendadas (ShedLock). Com mais de uma instancia da API no ar, as duas acordariam as 03:00 e
-- anonimizariam os mesmos clientes (o resultado e igual, mas e trabalho repetido e log duplicado). Antes de rodar,
-- cada instancia tenta gravar/atualizar a linha da rotina aqui; so quem conseguir executa.
--
-- name:       nome da rotina (ex.: anonimizacao-lgpd).
-- lock_until: ate quando a trava vale (se a instancia cair no meio, a trava expira sozinha).
-- locked_at:  quando a trava foi pega.  locked_by: qual instancia (hostname) pegou.
-- Nomes de coluna fixos: sao os que o ShedLock usa.

CREATE TABLE tb_shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until DATETIME(3)  NOT NULL,
    locked_at  DATETIME(3)  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
