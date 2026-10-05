package com.br.rkfbank.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.dto.response.cliente.EnderecoResponseDto;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.services.ClienteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    @Test
    void deveRetornar201AoCadastrarCliente() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva",
                "52998224725",
                "joao@rkfbank.com",
                "11999998888",
                "01001000",
                "123",
                "Apto 45"
        );

        when(clienteService.cadastrar(request)).thenReturn(clienteResponse());

        ResponseEntity<ClienteResponseDto> response = clienteController.cadastrar(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Joao Silva", response.getBody().nomeCompleto());
    }

    @Test
    void deveListarClientesSemPaginacao() {
        when(clienteService.listarTodos()).thenReturn(List.of(clienteResponse()));

        ResponseEntity<?> response = clienteController.listarClientes(false, 0, 10);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(List.class, response.getBody());
    }

    @Test
    void deveListarClientesComPaginacao() {
        when(clienteService.listarPaginado(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(clienteResponse()), PageRequest.of(0, 10), 1));

        ResponseEntity<?> response = clienteController.listarClientes(true, 0, 10);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(Page.class, response.getBody());
    }

    @Test
    void deveRejeitarPaginacaoComPaginaNegativa() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> clienteController.listarClientes(true, -1, 10)
        );

        assertEquals("page", exception.getCampo());
    }

    @Test
    void deveRejeitarPaginacaoComTamanhoForaDoLimite() {
        RequisicaoInvalidaException exception = assertThrows(
                RequisicaoInvalidaException.class,
                () -> clienteController.listarClientes(true, 0, 101)
        );

        assertEquals("size", exception.getCampo());
    }

    private ClienteResponseDto clienteResponse() {
        EnderecoResponseDto endereco = new EnderecoResponseDto(
                "01001-000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP",
                "3550308",
                "123",
                "Apto 45"
        );

        return new ClienteResponseDto(
                UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e"),
                "Joao Silva",
                "52998224725",
                "joao@rkfbank.com",
                "11999998888",
                endereco
        );
    }
}
