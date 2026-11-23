-- Estrutura do Flyway no RKF Bank. BASELINE POSTGRESQL (novembro/2026): as migracoes V1 a V15 foram reescritas do MySQL
-- para o PostgreSQL ANTES de existir qualquer banco PostgreSQL (o banco novo nasce vazio e o historico antigo do MySQL
-- nao e levado junto; ver planing/decisoes.md, decisao da migracao para PostgreSQL). Os numeros V1..V15 foram mantidos
-- de proposito: docs, testes e o README continuam dizendo a mesma coisa.
--   db/migration -> schema, uma tabela por versao, roda em TODO ambiente (dev, teste, producao).
--   db/seed      -> dados de demonstracao (Fellipe e Renan), so onde spring.flyway.locations incluir a pasta.
-- Regra daqui para frente (volta a valer a partir do baseline): migracao aplicada nunca se edita (o Flyway guarda o
-- checksum). Mudou algo? V16__, V17__...
--
-- Tipos seguem o que o Hibernate espera para validar as entidades (ddl-auto=validate):
-- UUID -> UUID nativo, BigDecimal -> NUMERIC(19,2), boolean -> BOOLEAN, Instant -> TIMESTAMP(6) WITH TIME ZONE
-- (timestamptz: guarda o instante absoluto, sem depender do fuso da JVM).
-- Tudo aqui roda no PostgreSQL 17 e no H2 2.5 (MODE=PostgreSQL) dos testes.

-- Endereco vem do ViaCEP no cadastro; cada cliente tem o seu (OneToOne em tb_cliente).
CREATE TABLE tb_endereco (
    id          UUID NOT NULL,
    cep         VARCHAR(9)   NOT NULL,
    logradouro  VARCHAR(255) NOT NULL,
    bairro      VARCHAR(255) NOT NULL,
    cidade      VARCHAR(255) NOT NULL,
    uf          VARCHAR(2)   NOT NULL,
    ibge        VARCHAR(255) NOT NULL,
    numero      VARCHAR(255) NOT NULL,
    complemento VARCHAR(255),
    CONSTRAINT pk_endereco PRIMARY KEY (id)
);
