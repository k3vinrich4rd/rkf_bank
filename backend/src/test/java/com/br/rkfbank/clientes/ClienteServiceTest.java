package com.br.rkfbank.clientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.br.rkfbank.autenticacao.CredencialService;
import com.br.rkfbank.clientes.dto.request.CadastroClienteRequestDto;
import com.br.rkfbank.clientes.dto.response.ClienteResponseDto;
import com.br.rkfbank.compartilhado.erros.ClienteNaoEncontradoException;
import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import com.br.rkfbank.compartilhado.erros.ConflitoException;
import com.br.rkfbank.compartilhado.erros.RequisicaoInvalidaException;
import com.br.rkfbank.titular.TermosDeUso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ConsultaCep consultaCep;

    // A credencial (login + hash BCrypt) e do modulo autenticacao: aqui so importa que o cadastro pede uma.
    @Mock
    private CredencialService credencialService;

    // Objeto de verdade (nao mock): so guarda a versao vigente dos termos.
    @Spy
    private TermosDeUso termosDeUso = new TermosDeUso("2026-10-23");

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void deveCriarACredencialDoClienteAoCadastrar() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "529.982.247-25", "joao@email.com", "11999998888", "01001000", "123", null, "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(clienteRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(consultaCep.buscar("01001000")).thenReturn(new EnderecoDoCep(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308"
        ));
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.cadastrar(request);

        // Login = CPF so com digitos; a senha vai em texto para o CredencialService, que grava so o hash.
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(credencialService).criarParaCliente(captor.capture(), eq("52998224725"), eq("senha-forte-1"));
        assertNotNull(captor.getValue());
        assertEquals("52998224725", captor.getValue().getCpf());
    }

    @Test
    void deveRecusarSenhaComMaisDe72BytesAntesDeConsultarOCep() {
        // 40 caracteres "ç" cabem no @Size(max = 72), mas ocupam 80 bytes em UTF-8.
        String senhaComAcentos = "ç".repeat(40);
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", null, senhaComAcentos, true, null
        );
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(clienteRepository.existsByEmail("joao@email.com")).thenReturn(false);

        RequisicaoInvalidaException ex = assertThrows(RequisicaoInvalidaException.class,
                () -> clienteService.cadastrar(request));

        assertEquals("senha", ex.getCampo());
        // a credencial so foi consultada (CPF ja e login de alguem?); nada foi gravado nem cifrado.
        verifyNoInteractions(consultaCep);
        verify(credencialService, never()).criarParaCliente(any(), any(), any());
    }

    @Test
    void deveRecusarCpfQueJaELoginDeUmUsuarioSemCadastroDeCliente() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "529.982.247-25", "joao@email.com", "11999998888", "01001000", "123", null, "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(credencialService.loginEmUso("52998224725")).thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));

        assertEquals(CodigoErroEnum.CPF_JA_CADASTRADO, ex.getCodigo());
        verifyNoInteractions(consultaCep);
        verify(credencialService, never()).criarParaCliente(any(), any(), any());
    }

    @Test
    void naoDeveExporSenhaNemCpfNoToStringDoRequest() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", null, "senha-forte-1", true, null
        );

        String texto = request.toString();

        assertFalse(texto.contains("senha-forte-1"));
        assertFalse(texto.contains("52998224725"));
    }

    @Test
    void deveCadastrarClienteComEnderecoDoCep() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva",
                "52998224725",
                "joao@email.com",
                "11999998888",
                "01001000",
                "123",
                "Apto 45",
                "senha-forte-1", true, null
        );

        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(consultaCep.buscar("01001000")).thenReturn(new EnderecoDoCep(
                "01001-000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP",
                "3550308"
        ));

        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente salvo = invocation.getArgument(0);
            ReflectionTestUtils.setField(salvo, "id", UUID.fromString("d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e"));
            return salvo;
        });

        ClienteResponseDto response = clienteService.cadastrar(request);

        assertNotNull(response);
        assertEquals("Joao Silva", response.nomeCompleto());
        assertEquals("***.982.247-**", response.cpf());
        assertEquals("2026-10-23", response.termosVersao());
        assertNotNull(response.termosAceitosEm());
        assertEquals("Praca da Se", response.endereco().logradouro());
    }

    @Test
    void deveLancarConflitoQuandoCpfDuplicado() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", "Apto 45", "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(true);

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));
    }

    @Test
    void deveLancarConflitoQuandoEmailDuplicado() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", "Apto 45", "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));
    }

    @Test
    void deveSalvarEmailEmMinusculasESemEspacos() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "  Joao.Silva@Email.COM ", "11999998888", "01001000", "123", "Apto 45", "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail("joao.silva@email.com")).thenReturn(false);
        when(consultaCep.buscar("01001000")).thenReturn(new EnderecoDoCep(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308"
        ));
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDto response = clienteService.cadastrar(request);

        assertEquals("joao.silva@email.com", response.email());
    }

    @Test
    void deveLancarConflitoQuandoEmailDuplicadoComMaiusculas() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "JOAO@EMAIL.COM", "11999998888", "01001000", "123", "Apto 45", "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));
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
    void deveNormalizarCepAntesDeConsultarAPorta() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva",
                "52998224725",
                "joao@email.com",
                "11999998888",
                "01001-000",
                "123",
                "Apto 45",
                "senha-forte-1", true, null
        );

        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(consultaCep.buscar("01001000")).thenReturn(new EnderecoDoCep(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308"
        ));
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.cadastrar(request);

        verify(consultaCep).buscar("01001000");
    }

    @Test
    void deveConverterConflitoDeUnicidadeNoInsertEm409() {
        CadastroClienteRequestDto request = new CadastroClienteRequestDto(
                "Joao Silva", "52998224725", "joao@email.com", "11999998888", "01001000", "123", "Apto 45", "senha-forte-1", true, null
        );
        when(clienteRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(clienteRepository.existsByEmail(request.email())).thenReturn(false);
        when(consultaCep.buscar("01001000")).thenReturn(new EnderecoDoCep(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", "3550308"
        ));
        when(clienteRepository.saveAndFlush(any(Cliente.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate CPF"));

        assertThrows(ConflitoException.class, () -> clienteService.cadastrar(request));
    }
}
