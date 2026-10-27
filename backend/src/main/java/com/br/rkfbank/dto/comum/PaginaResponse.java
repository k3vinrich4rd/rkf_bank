package com.br.rkfbank.dto.comum;

import org.springframework.data.domain.Page;

import java.util.List;

// Formato de pagina do contrato, sob controle da API. Serializar o Page do Spring direto expoe o PageImpl
// (pageable, sort...) e muda quando o Spring Data muda. Os nomes dos campos sao os que o frontend ja le
// (frontend/lib/api/types.ts -> Page<T>), entao trocar o Page por este record nao quebra nenhuma tela.
public record PaginaResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    boolean first,
    boolean last
) {

    // Copia defensiva: a pagina devolvida nao muda se alguem mexer na lista original (mesmo motivo do ErroResponse).
    public PaginaResponse {
        content = List.copyOf(content);
    }

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
