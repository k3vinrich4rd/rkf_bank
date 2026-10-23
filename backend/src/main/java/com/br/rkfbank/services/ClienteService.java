package com.br.rkfbank.services;

import com.br.rkfbank.clients.viacep.ViaCepClient;
import com.br.rkfbank.clients.viacep.ViaCepResponseDto;
import com.br.rkfbank.dto.request.cliente.CadastroClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.dto.response.cliente.EnderecoResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.entities.enums.PerfilEnum;
import com.br.rkfbank.exceptions.ClienteNaoEncontradoException;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.repositories.ClienteRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// Cadastro de clientes. O cadastro cria tres coisas na MESMA transacao: endereco (do ViaCEP), cliente e a
// credencial de login (Usuario com o hash BCrypt). Falhou qualquer uma, nenhuma fica no banco.
@Service
public class ClienteService {

    private static final Logger log = LogManager.getLogger(ClienteService.class);

    // Limite do BCrypt: o que passa de 72 bytes ele nao le (o Spring Security recusa com excecao).
    static final int MAX_BYTES_SENHA = 72;

    private final ClienteRepository clienteRepository;
    private final ViaCepClient viaCepClient;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(
            ClienteRepository clienteRepository,
            ViaCepClient viaCepClient,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.clienteRepository = clienteRepository;
        this.viaCepClient = viaCepClient;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Ordem pensada: primeiro as checagens baratas e locais (CPF/e-mail repetidos, tamanho da senha), depois a
    // chamada externa ao ViaCEP (lenta e sujeita a falha). Assim um cadastro que ja seria recusado nem gasta a chamada.
    @Transactional
    public ClienteResponseDto cadastrar(CadastroClienteRequestDto request) {
        // Normaliza o CPF para manter somente dígitos antes das validações de banco.
        String cpfSanitizado = request.cpf().replaceAll("\\D", "");

        // Regra de unicidade de CPF antes de consultar qualquer serviço externo.
        // CPF e email ficam fora dos logs por serem dados pessoais.
        if (clienteRepository.existsByCpf(cpfSanitizado)) {
            log.warn("Cadastro de cliente recusado: CPF ja cadastrado");
            throw new ConflitoException("CPF ja cadastrado");
        }

        // Email guardado sempre em minúsculas e sem espaços nas pontas (a V2 do Flyway fez o mesmo com os já salvos).
        // Assim a unicidade não depende da collation do banco: o MySQL ignora maiúsculas, o H2 dos testes não.
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);

        // Regra de unicidade de email para evitar conflito de cadastro.
        if (clienteRepository.existsByEmail(emailNormalizado)) {
            log.warn("Cadastro de cliente recusado: email ja cadastrado");
            throw new ConflitoException("Email ja cadastrado");
        }

        // @Size conta caracteres; o BCrypt conta bytes (acento ocupa 2). Checado antes de chamar o ViaCEP.
        garantirLimiteDoBcrypt("senha", request.senha());

        // Normaliza o CEP para manter somente dígitos antes de chamar o ViaCEP.
        String cep = request.cep().replaceAll("\\D", "");

        // Camada de integração externa: resolve dados oficiais do endereço.
        ViaCepResponseDto viaCep = viaCepClient.buscarPorCep(cep);

        // Montagem da entidade de domínio com dados da request + resposta do ViaCEP.
        Cliente cliente = montarCliente(request, viaCep);

        // Altera o CPF da entidade para salvar a versão puramente numérica no banco de dados.
        cliente.setCpf(cpfSanitizado);
        cliente.setEmail(emailNormalizado);

        // Normaliza o telefone para manter apenas dígitos antes de salvar no banco.
        String telefoneSanitizado = request.telefone().replaceAll("\\D", "");
        cliente.setTelefone(telefoneSanitizado);

        // Persistência transacional: salva cliente/endereço e retorna no formato de API.
        Cliente salvo;
        try {
            salvo = clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException ex) {
            // Cadastro concorrente passou pelas checagens acima e esbarrou na constraint unica do banco.
            // A mensagem do driver traz o valor duplicado (CPF/email), por isso nao entra no log.
            log.warn("Cadastro de cliente recusado: violacao de unicidade no banco");
            throw new ConflitoException("CPF ou email ja cadastrado");
        }

        // Credencial de acesso na mesma transacao: se falhar, o cliente tambem e desfeito (rollback).
        // So o hash vai para o banco; a senha em texto termina aqui.
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfSanitizado);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(PerfilEnum.CLIENTE);
        usuario.setCliente(salvo);
        usuarioRepository.save(usuario);

        log.info("Cliente {} cadastrado (cidade={}/{})", salvo.getId(), viaCep.localidade(), viaCep.uf());
        return paraResponse(salvo);
    }

    // @Size conta caracteres; o BCrypt conta bytes (acento ocupa 2). Usado no cadastro e nas trocas de senha.
    static void garantirLimiteDoBcrypt(String campo, String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_SENHA) {
            throw new RequisicaoInvalidaException(campo, campo + " deve ter no maximo " + MAX_BYTES_SENHA + " bytes");
        }
    }

    // Busca unica por id, reaproveitada por outros services, sempre com o mesmo 404.
    public Cliente buscar(UUID id) {
        // Busca centralizada para reutilizar a mesma exceção de 404 em outras camadas.
        return clienteRepository.findById(id).orElseThrow(() -> {
            log.warn("Cliente {} nao encontrado", id);
            return new ClienteNaoEncontradoException();
        });
    }

    // Lista completa (so ADMIN). Converte para DTO dentro da transacao, enquanto o endereco (lazy) ainda carrega.
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> listarTodos() {
        // Leitura sem paginação para telas simples.
        List<ClienteResponseDto> clientes = clienteRepository.findAll().stream().map(this::paraResponse).toList();
        log.debug("Listagem de clientes retornou {} registros", clientes.size());
        return clientes;
    }

    // Pagina do banco: Page.map converte cada cliente mantendo total e numero da pagina.
    @Transactional(readOnly = true)
    public Page<ClienteResponseDto> listarPaginado(Pageable pageable) {
        // Leitura paginada para reduzir payload em listas grandes.
        Page<ClienteResponseDto> pagina = clienteRepository.findAll(pageable).map(this::paraResponse);
        log.debug("Listagem paginada de clientes: pagina {} com {} de {} registros",
                pageable.getPageNumber(), pagina.getNumberOfElements(), pagina.getTotalElements());
        return pagina;
    }

    private static Cliente montarCliente(CadastroClienteRequestDto request, ViaCepResponseDto viaCep) {
        Endereco endereco = new Endereco();
        // Garante que o CEP salvo no banco de dados esteja com hífen conforme o padrão do ViaCEP/README
        String cepComHifen = viaCep.cep() != null && !viaCep.cep().contains("-") && viaCep.cep().length() == 8
                ? viaCep.cep().replaceAll("(\\d{5})(\\d{3})", "$1-$2")
                : viaCep.cep();

        endereco.setCep(cepComHifen);
        endereco.setLogradouro(viaCep.logradouro());
        endereco.setBairro(viaCep.bairro());
        endereco.setCidade(viaCep.localidade());
        endereco.setUf(viaCep.uf());
        endereco.setIbge(viaCep.ibge());
        endereco.setNumero(request.numero());
        endereco.setComplemento(request.complemento());

        Cliente cliente = new Cliente();
        cliente.setNomeCompleto(request.nomeCompleto());
        cliente.setCpf(request.cpf());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(endereco);
        return cliente;
    }

    // Pacote: o TitularService devolve o cadastro no mesmo formato.
    ClienteResponseDto paraResponse(Cliente cliente) {
        Endereco endereco = cliente.getEndereco();
        EnderecoResponseDto enderecoResponse = new EnderecoResponseDto(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getIbge(),
                endereco.getNumero(),
                endereco.getComplemento()
        );

        String cpfFormatado = formatarCpf(cliente);
        String telefoneFormatado = formatarTelefone(cliente);

        return new ClienteResponseDto(
                cliente.getId(),
                cliente.getNomeCompleto(),
                cpfFormatado,
                cliente.getEmail(),
                telefoneFormatado,
                enderecoResponse
        );
    }

    private static String formatarCpf(Cliente cliente) {
        String cpfFormatado = cliente.getCpf();
        if (cpfFormatado != null && cpfFormatado.length() == 11) {
            cpfFormatado = cpfFormatado.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return cpfFormatado;
    }

    private static String formatarTelefone(Cliente cliente) {
        String tel = cliente.getTelefone();
        if (tel != null) {
            if (tel.length() == 11) {
                // Celulares: (XX) XXXXX-XXXX
                return tel.replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
            } else if (tel.length() == 10) {
                // Fixos: (XX) XXXX-XXXX
                return tel.replaceAll("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
            }
        }
        return tel;
    }
}
