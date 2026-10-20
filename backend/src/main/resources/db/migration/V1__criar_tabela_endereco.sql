-- Estrutura do Flyway no RKF Bank (reorganizada em 20/10/2026, antes de existir qualquer banco de producao):
--   db/migration -> schema, uma tabela por versao, roda em TODO ambiente (dev, teste, producao).
--   db/seed      -> dados de demonstracao (Felipe e Renan), so onde spring.flyway.locations incluir a pasta.
-- Regra daqui para frente: migracao aplicada nunca se edita (o Flyway guarda o checksum). Mudou algo? V6__, V7__...
--
-- Tipos seguem o que o Hibernate espera para validar as entidades (ddl-auto=validate):
-- UUID -> BINARY(16), BigDecimal -> DECIMAL(19,2), boolean -> BIT, Instant -> DATETIME(6).
-- Tudo aqui roda no MySQL 8.4 e no H2 2.3 dos testes.

-- Endereco vem do ViaCEP no cadastro; cada cliente tem o seu (OneToOne em tb_cliente).
CREATE TABLE tb_endereco (
    id          BINARY(16)   NOT NULL,
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
