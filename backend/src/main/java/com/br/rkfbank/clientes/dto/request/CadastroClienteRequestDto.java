package com.br.rkfbank.clientes.dto.request;

import com.br.rkfbank.compartilhado.validacao.CpfValido;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Corpo do POST /clientes (cadastro publico). Validacao de FORMATO aqui (Bean Validation); regras que dependem
// do banco (CPF ou e-mail ja usados) ficam no ClienteService, porque o DTO nao enxerga o banco.
// O endereco completo nao vem do cliente: so CEP, numero e complemento; o resto vem do ViaCEP.
public record CadastroClienteRequestDto(
        @NotBlank(message = "nomeCompleto é obrigatório")
        @Size(max = 120, message = "nomeCompleto deve ter no máximo 120 caracteres")
        String nomeCompleto,
        @NotBlank(message = "cpf é obrigatório")
        @CpfValido(message = "cpf inválido")
        String cpf,
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "telefone é obrigatório")
        String telefone,
        @NotBlank(message = "cep é obrigatório")
        String cep,
        @NotBlank(message = "numero é obrigatório")
        String numero,
        @Size(max = 80, message = "complemento deve ter no máximo 80 caracteres")
        String complemento,
        // 72 e o limite do BCrypt (em bytes; o ClienteService confere os bytes, porque acento ocupa mais de 1).
        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha,
        // LGPD: o cadastro so existe com o aceite dos Termos de Uso e da Politica de Privacidade (fica registrado com
        // a versao e o instante). termosVersao e opcional; vindo, tem que ser a vigente (GET /termos), senao 422.
        @NotNull(message = "aceiteTermos é obrigatório")
        @AssertTrue(message = "aceite os Termos de Uso e a Política de Privacidade para criar a conta")
        Boolean aceiteTermos,
        @Size(max = 20, message = "termosVersao deve ter no máximo 20 caracteres")
        String termosVersao
) {

    // O toString padrao do record imprime todos os campos; sem isto, um log do DTO vazaria senha e CPF.
    @Override
    public String toString() {
        return "CadastroClienteRequestDto[nomeCompleto=***, cpf=***, email=***, telefone=***, cep=" + cep
                + ", numero=" + numero + ", complemento=" + complemento + ", senha=***, aceiteTermos=" + aceiteTermos + ", termosVersao=" + termosVersao + "]";
    }
}
