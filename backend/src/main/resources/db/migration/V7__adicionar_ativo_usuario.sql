-- Acesso revogavel: um ADMIN pode desativar (e reativar) o login de qualquer usuario.
-- Desativado: o login falha e todo token ja emitido para ele passa a dar 401 na hora (a API confere tb_usuario
-- a cada requisicao, ConversorTokenUsuario). Banco nao apaga usuario: o historico (tb_idempotencia) aponta para ele.
-- DEFAULT TRUE: todo usuario que ja existe continua entrando. BOOLEAN nativo.

ALTER TABLE tb_usuario ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
