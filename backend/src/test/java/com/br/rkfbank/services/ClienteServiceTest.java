package com.br.rkfbank.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.rkfbank.clients.viacep.ClienteViaCep;
import com.br.rkfbank.clients.viacep.ResponseViaCepDto;
import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.repositories.ClienteRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteViaCep clienteViaCep;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void deveCadastrarClienteComEnderecoViaCep() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva",
                "52998224725",
                "joao@email.com",
                "11999998888",
                "01001000",
                "123",
                "Apto 45"
        );

        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(clienteViaCep.buscarPorCep("01001000")).thenReturn(new ResponseViaCepDto(
                "01001-000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP",
                "3550308",
                false
        ));

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente salvo = invocation.getArgument(0);
            ReflectionTestUtils.setField(salvo, "id", UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e"));
            return salvo;
        });

        ClienteResponseDto response = clienteService.cadastrar(request);

        assertNotNull(response);
        assertEquals("Joao Silva", response.nomeCompleto());
        assertEquals("52998224725", response.cpf());
        assertEquals("Praca da Se", response.endereco().logradouro());
    }

    @Test
    void deveLancarConflitoQuandoCpfDuplicado() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", "Apto 45"
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(true);

        assertThrows(NegocioException.class, () -> clienteService.cadastrar(request));
    }

    @Test
    void deveLancarConflitoQuandoEmailDuplicado() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", "Apto 45"
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(NegocioException.class, () -> clienteService.cadastrar(request));
    }

    @Test
    void deveLancarQuandoClienteNaoExisteNaBusca() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.buscar(id));
    }

    @Test
    void deveBuscarClienteQuandoExiste() {
        UUID id = UUID.randomUUID();
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", id);
        when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));

        Cliente encontrado = clienteService.buscar(id);

        assertEquals(id, encontrado.getId());
    }

    @Test
    void deveListarClientesSemPaginacao() {
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");
        cliente.setEndereco(endereco);
        cliente.setNomeCompleto("Joao");
        cliente.setCpf("52998224725");
        cliente.setEmail("joao@rkfbank.com");
        cliente.setTelefone("11999998888");

        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        List<ClienteResponseDto> response = clienteService.listarTodos();

        assertEquals(1, response.size());
    }

    @Test
    void deveListarClientesComPaginacao() {
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        Endereco endereco = new Endereco();
        endereco.setCep("01001-000");
        endereco.setLogradouro("Praca da Se");
        endereco.setBairro("Se");
        endereco.setCidade("Sao Paulo");
        endereco.setUf("SP");
        endereco.setIbge("3550308");
        endereco.setNumero("123");
        cliente.setEndereco(endereco);
        cliente.setNomeCompleto("Joao");
        cliente.setCpf("52998224725");
        cliente.setEmail("joao@rkfbank.com");
        cliente.setTelefone("11999998888");

        when(clienteRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(cliente), PageRequest.of(0, 10), 1));

        assertEquals(1, clienteService.listarPaginado(PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void deveNormalizarCepAntesDeConsultarViaCep() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva",
                "52998224725",
                "joao@email.com",
                "11999998888",
                "01001-000",
                "123",
                "Apto 45"
        );

        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(clienteViaCep.buscarPorCep("01001000")).thenReturn(new ResponseViaCepDto(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308", false
        ));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.cadastrar(request);

        verify(clienteViaCep).buscarPorCep("01001000");
    }
}
