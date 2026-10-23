-- Limite de tentativas de login guardado no banco (antes ficava na memoria da API: reiniciar zerava, e com mais de
-- uma instancia cada uma contava a sua parte, entao o atacante ganhava 5 chutes por instancia).
--
-- chave_login:    SHA-256 do CPF digitado (hex). O CPF em si nao fica: muitos chutes sao de CPFs que nem sao
--                 clientes, e o banco nao deve guardar dado pessoal de quem nao tem relacao com ele (LGPD).
-- falhas:         quantos erros dentro da janela de 15 minutos.
-- primeira_falha: quando a janela comecou (o bloqueio vale ate primeira_falha + 15 min).

CREATE TABLE tb_tentativa_login (
    chave_login    CHAR(64)    NOT NULL,
    falhas         INT         NOT NULL,
    primeira_falha DATETIME(6) NOT NULL,
    CONSTRAINT pk_tentativa_login PRIMARY KEY (chave_login)
);

-- A limpeza diaria apaga as janelas vencidas.
CREATE INDEX idx_tentativa_login_primeira_falha ON tb_tentativa_login (primeira_falha);
