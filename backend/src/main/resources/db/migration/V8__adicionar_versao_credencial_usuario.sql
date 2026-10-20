-- Troca e redefinicao de senha. versao_credencial sobe a cada troca e vai dentro do token (claim versaoCredencial).
-- A cada requisicao o ConversorTokenUsuario compara as duas: trocou a senha, todo token emitido antes vira 401 na hora
-- (quem roubou um token perde o acesso junto). Contador e nao data: nao depende de relogio nem de precisao de segundos.
-- DEFAULT 0: usuarios existentes comecam na versao 0, a mesma que um token sem a claim representa.

ALTER TABLE tb_usuario ADD COLUMN versao_credencial INT NOT NULL DEFAULT 0;
