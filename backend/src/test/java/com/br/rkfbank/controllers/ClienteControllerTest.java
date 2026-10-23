package com.br.rkfbank.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.dto.response.cliente.EnderecoResponseDto;
import com.br.rkfbank.services.ClienteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest extends ControllerWebMvcBase {

    // CPF valido de exemplo (digitos verificadores certos), nunca de uma pessoa da equipe.
    private static final String CADASTRO = """
            {"nomeCompleto":"Joao Silva","cpf":"52998224725","email":"joao@rkfbank.com","telefone":"11999998888",
             "cep":"01001000","numero":"123","complemento":"Apto 45","senha":"senha-forte-1","aceiteTermos":true}
            """;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void deveRetornar201AoCadastrarClienteSemToken() throws Exception {
        when(clienteService.cadastrar(any(CadastroClienteRequestDto.class))).thenReturn(clienteResponse());

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCompleto").value("Joao Silva"))
                .andExpect(jsonPath("$.cpf").value("***.982.247-**"))
                .andExpect(jsonPath("$.endereco.cep").value("01001-000"))
                // A senha nunca volta na resposta.
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void cadastroSemAceiteDosTermosNemChegaAoService() throws Exception {
        String semAceite = CADASTRO.replace("\"aceiteTermos\":true", "\"aceiteTermos\":false");

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(semAceite))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos[0].campo").value("aceiteTermos"));
        verifyNoInteractions(clienteService);
    }

    @Test
    void jsonQuebradoResponde400ComCodigoDoContrato() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"nomeCompleto\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").exists());
        verifyNoInteractions(clienteService);
    }

    @Test
    void listarClientesESoDoAdmin() throws Exception {
        mockMvc.perform(get("/clientes").with(tokenDoCliente()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveListarClientesSemPaginacao() throws Exception {
        when(clienteService.listarTodos()).thenReturn(List.of(clienteResponse()));

        mockMvc.perform(get("/clientes").with(tokenDoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deveListarClientesComPaginacao() throws Exception {
        when(clienteService.listarPaginado(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(clienteResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/clientes").param("page", "0").param("size", "10").with(tokenDoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void deveRejeitarPaginacaoComPaginaNegativa() throws Exception {
        mockMvc.perform(get("/clientes").param("page", "-1").param("size", "10").with(tokenDoAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("page"));
    }

    @Test
    void deveRejeitarPaginacaoComTamanhoForaDoLimite() throws Exception {
        mockMvc.perform(get("/clientes").param("page", "0").param("size", "101").with(tokenDoAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("size"));
    }

    private ClienteResponseDto clienteResponse() {
        EnderecoResponseDto endereco = new EnderecoResponseDto("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP",
                "3550308", "123", "Apto 45");
        return new ClienteResponseDto(CLIENTE_ID, "Joao Silva", "***.982.247-**", "joao@rkfbank.com", "11999998888",
                endereco, "2026-10-23", null);
    }
}
