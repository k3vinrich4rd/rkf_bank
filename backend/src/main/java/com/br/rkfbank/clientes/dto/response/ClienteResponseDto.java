package com.br.rkfbank.clientes.dto.response;

import java.time.Instant;
import java.util.UUID;

// Cadastro devolvido pela API. CPF MASCARADO (***.982.247-**, LGPD: a tela nao precisa dele inteiro; o titular sabe o
// proprio CPF) e telefone formatado. A senha (e o hash) nunca entram aqui. termos*: o aceite registrado no cadastro.
public record ClienteResponseDto(
    UUID id,
    String nomeCompleto,
    String cpf,
    String email,
    String telefone,
    EnderecoResponseDto endereco,
    String termosVersao,
    Instant termosAceitosEm
) {}