-- Senha redefinida pelo ADMIN (PATCH /admin/usuarios/{id}/senha) e uma senha TEMPORARIA: o ADMIN a conhece, porque foi
-- ele quem a passou para a pessoa. Com esta flag, o proximo login so serve para trocar a senha; qualquer outra rota
-- responde 403 TROCA_DE_SENHA_OBRIGATORIA ate a pessoa escolher uma senha que so ela sabe.
-- BIT como a coluna ativo (V7): o mesmo tipo no MySQL e no H2. Todo usuario existente comeca sem a obrigacao.

ALTER TABLE tb_usuario ADD COLUMN deve_trocar_senha BIT NOT NULL DEFAULT FALSE;
