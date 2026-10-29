package com.br.rkfbank.compartilhado.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// Records com lista: a resposta nao pode mudar depois de montada (copia defensiva no construtor compacto).
class ImutabilidadeRespostasTest {

    @Test
    void erroResponseNaoMudaQuandoAListaOriginalMuda() {
        List<ErroCampoResponse> campos = new ArrayList<>(List.of(new ErroCampoResponse("valor", "valor é obrigatório")));
        ErroResponse erro = new ErroResponse(Instant.now(), 400, "Bad Request", CodigoErroEnum.CAMPOS_INVALIDOS,
                "Requisição inválida", "/contas", campos);

        campos.add(new ErroCampoResponse("cpf", "inválido"));

        assertThat(erro.campos()).extracting(ErroCampoResponse::campo).containsExactly("valor");
        assertThatThrownBy(() -> erro.campos().add(new ErroCampoResponse("x", "y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void erroResponseTrocaCamposNulosPorListaVazia() {
        ErroResponse erro = new ErroResponse(Instant.now(), 404, "Not Found", CodigoErroEnum.CONTA_NAO_ENCONTRADA,
                "Conta não encontrada", "/contas/1", null);

        assertThat(erro.campos()).isEmpty();
    }

    @Test
    void paginaResponseNaoDeixaAlterarOConteudo() {
        List<String> itens = new ArrayList<>(List.of("a", "b"));
        PaginaResponse<String> pagina = PaginaResponse.de(new PageImpl<>(itens, PageRequest.of(0, 2), 2));

        itens.clear();

        assertThat(pagina.content()).containsExactly("a", "b");
        assertThatThrownBy(() -> pagina.content().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }
}
