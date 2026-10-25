package com.br.rkfbank.entities.enums;

// Perfil de acesso de quem faz login. Vai no token (claim "perfil") e vira ROLE_CLIENTE ou ROLE_ADMIN
// para o Spring Security decidir quem pode cada rota. Gravado como texto em tb_usuario.perfil (CHECK na V5).
public enum PerfilEnum {

    CLIENTE("Cliente do banco"),
    ADMIN("Administrador");

    private final String descricao;

    PerfilEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
