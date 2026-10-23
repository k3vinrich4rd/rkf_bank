package com.br.rkfbank.services;

import com.br.rkfbank.clients.viacep.ViaCepClient;
import com.br.rkfbank.clients.viacep.ViaCepResponseDto;
import com.br.rkfbank.config.LgpdPropriedades;
import com.br.rkfbank.dto.request.cliente.AtualizarClienteRequestDto;
import com.br.rkfbank.dto.response.cliente.ClienteResponseDto;
import com.br.rkfbank.dto.response.cliente.EncerramentoResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.entities.Endereco;
import com.br.rkfbank.entities.Usuario;
import com.br.rkfbank.exceptions.CodigoErroEnum;
import com.br.rkfbank.exceptions.RegraNegocioException;
import com.br.rkfbank.exceptions.ConflitoException;
import com.br.rkfbank.exceptions.RequisicaoInvalidaException;
import com.br.rkfbank.repositories.ClienteRepository;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.UsuarioRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Direitos do titular (LGPD, art. 18): ver, corrigir e encerrar o proprio cadastro, e a anonimizacao no fim do prazo.
// O titular vem sempre do token (UsuarioLogado.clienteId), nunca de um id na URL: ninguem acessa o cadastro de outro.
@Service
public class TitularService {

    private static final Logger log = LogManager.getLogger(TitularService.class);

    private static final ZoneId FUSO_DO_BANCO = ZoneId.of("America/Sao_Paulo");
    static final String MOTIVO_DA_RETENCAO = "Lei 9.613/1998, art. 10: cadastro e operacoes guardados pelo prazo legal "
            + "depois do fim do relacionamento; em seguida, os dados pessoais sao anonimizados";
    static final String ANONIMIZADO = "anonimizado";

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final ViaCepClient viaCepClient;
    private final ClienteService clienteService;
    private final PasswordEncoder passwordEncoder;
    private final LgpdPropriedades lgpd;

    public TitularService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                          ContaRepository contaRepository, ViaCepClient viaCepClient, ClienteService clienteService,
                          PasswordEncoder passwordEncoder, LgpdPropriedades lgpd) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.contaRepository = contaRepository;
        this.viaCepClient = viaCepClient;
        this.clienteService = clienteService;
        this.passwordEncoder = passwordEncoder;
        this.lgpd = lgpd;
    }

    // Direito de acesso: tudo o que o banco guarda do cadastro do titular.
    @Transactional(readOnly = true)
    public ClienteResponseDto meusDados(UsuarioLogado usuario) {
        return clienteService.paraResponse(titular(usuario));
    }

    // Direito de correcao. Cada campo e opcional; a validacao de formato ja passou no DTO.
    @Transactional
    public ClienteResponseDto corrigir(AtualizarClienteRequestDto request, UsuarioLogado usuario) {
        if (request.vazio()) {
            throw new RequisicaoInvalidaException("corpo", "envie pelo menos um campo para corrigir");
        }
        Cliente cliente = titular(usuario);
        List<String> corrigidos = new ArrayList<>();

        if (request.nomeCompleto() != null) {
            String nome = request.nomeCompleto().trim();
            if (nome.isEmpty()) {
                throw new RequisicaoInvalidaException("nomeCompleto", "nomeCompleto nao pode ficar em branco");
            }
            cliente.setNomeCompleto(nome);
            corrigidos.add("nomeCompleto");
        }
        if (request.email() != null) {
            // Mesma normalizacao do cadastro: a unicidade nao depende da collation do banco.
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(cliente.getEmail()) && clienteRepository.existsByEmail(email)) {
                log.warn("Correcao do cliente {} recusada: email ja cadastrado", cliente.getId());
                throw new ConflitoException(CodigoErroEnum.EMAIL_JA_CADASTRADO);
            }
            cliente.setEmail(email);
            corrigidos.add("email");
        }
        if (request.telefone() != null) {
            String telefone = request.telefone().replaceAll("\\D", "");
            if (telefone.length() < 10 || telefone.length() > 11) {
                throw new RequisicaoInvalidaException("telefone", "telefone deve ter DDD + 8 ou 9 digitos");
            }
            cliente.setTelefone(telefone);
            corrigidos.add("telefone");
        }
        corrigirEndereco(cliente.getEndereco(), request, corrigidos);

        try {
            clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException ex) {
            // Outro cadastro pegou o mesmo e-mail entre a checagem e o UPDATE. O valor nao entra no log.
            log.warn("Correcao do cliente {} recusada: violacao de unicidade no banco", cliente.getId());
            throw new ConflitoException(CodigoErroEnum.EMAIL_JA_CADASTRADO);
        }
        // So o nome dos campos: os valores sao dados pessoais.
        log.info("Cliente {} corrigiu o cadastro (campos={})", cliente.getId(), corrigidos);
        return clienteService.paraResponse(cliente);
    }

    private void corrigirEndereco(Endereco endereco, AtualizarClienteRequestDto request, List<String> corrigidos) {
        if (request.cep() != null) {
            if (request.numero() == null) {
                throw new RequisicaoInvalidaException("numero", "numero e obrigatorio quando o cep muda");
            }
            ViaCepResponseDto viaCep = viaCepClient.buscarPorCep(request.cep().replaceAll("\\D", ""));
            String cep = viaCep.cep() != null && viaCep.cep().matches("\\d{8}")
                    ? viaCep.cep().replaceAll("(\\d{5})(\\d{3})", "$1-$2")
                    : viaCep.cep();
            endereco.setCep(cep);
            endereco.setLogradouro(viaCep.logradouro());
            endereco.setBairro(viaCep.bairro());
            endereco.setCidade(viaCep.localidade());
            endereco.setUf(viaCep.uf());
            endereco.setIbge(viaCep.ibge());
            corrigidos.add("cep");
        }
        if (request.numero() != null) {
            endereco.setNumero(request.numero().trim());
            corrigidos.add("numero");
        }
        if (request.complemento() != null) {
            String complemento = request.complemento().trim();
            endereco.setComplemento(complemento.isEmpty() ? null : complemento);
            corrigidos.add("complemento");
        }
    }

    // Direito de eliminacao, do jeito que um banco pode cumprir: encerra agora, guarda pelo prazo legal e anonimiza.
    @Transactional
    public EncerramentoResponseDto encerrar(String senha, UsuarioLogado usuario) {
        Cliente cliente = titular(usuario);
        Usuario credencial = usuarioRepository.findById(usuario.usuarioId())
                .orElseThrow(() -> new RegraNegocioException(CodigoErroEnum.SEM_CADASTRO_CLIENTE));

        ClienteService.garantirLimiteDoBcrypt("senha", senha);
        if (!passwordEncoder.matches(senha, credencial.getSenhaHash())) {
            log.warn("Encerramento do cliente {} recusado: senha incorreta", cliente.getId());
            throw new RegraNegocioException(CodigoErroEnum.SENHA_INCORRETA);
        }

        List<Conta> contas = contaRepository.findByClienteId(cliente.getId());
        BigDecimal saldo = contas.stream().map(Conta::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (saldo.signum() != 0) {
            // Dinheiro nao some: o titular transfere o saldo antes. Valor fora do log (e dado financeiro dele).
            log.warn("Encerramento do cliente {} recusado: contas com saldo", cliente.getId());
            throw new RegraNegocioException(CodigoErroEnum.SALDO_NAO_ZERADO);
        }

        Instant agora = Instant.now();
        contas.forEach(conta -> conta.setAtiva(false));
        // Login e tokens caem na hora: o ConversorTokenUsuario confere "ativo" a cada requisicao.
        credencial.setAtivo(false);
        cliente.encerrar(agora);
        log.info("Cliente {} encerrou o cadastro ({} conta(s) desativada(s))", cliente.getId(), contas.size());

        LocalDate anonimizacao = agora.atZone(FUSO_DO_BANCO).toLocalDate().plus(lgpd.retencaoAposEncerramento());
        return new EncerramentoResponseDto(agora, anonimizacao, MOTIVO_DA_RETENCAO);
    }

    // Chamado pela RotinaAnonimizacao. Troca todo dado que identifica a pessoa por valores sem significado.
    // Mantem: contas, lancamentos (registro financeiro) e cidade/UF (estatistica, nao identifica sozinha).
    @Transactional
    public int anonimizarVencidos(Instant agora) {
        Instant limite = agora.atZone(ZoneOffset.UTC).minus(lgpd.retencaoAposEncerramento()).toInstant();
        List<Cliente> vencidos = clienteRepository.findByEncerradoEmBeforeAndAnonimizadoEmIsNull(limite);
        for (Cliente cliente : vencidos) {
            // 11 caracteres (tamanho de cpf e login) e nunca um CPF valido: comeca com letra.
            String marcador = "A" + cliente.getId().toString().replace("-", "").substring(0, 10);
            cliente.setNomeCompleto("Titular anonimizado");
            cliente.setCpf(marcador);
            cliente.setEmail(ANONIMIZADO + "-" + cliente.getId() + "@rkfbank.invalid");
            cliente.setTelefone(ANONIMIZADO);
            Endereco endereco = cliente.getEndereco();
            endereco.setCep("00000-000");
            endereco.setLogradouro(ANONIMIZADO);
            endereco.setBairro(ANONIMIZADO);
            endereco.setNumero("0");
            endereco.setComplemento(null);
            usuarioRepository.findByClienteId(cliente.getId()).ifPresent(credencial -> {
                credencial.setLogin(marcador);
                // Hash de uma senha que ninguem conhece: a conta nunca mais entra, nem se for reativada por engano.
                credencial.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                credencial.setAtivo(false);
            });
            cliente.marcarAnonimizado(agora);
            log.info("Cliente {} anonimizado (encerrado em {})", cliente.getId(), cliente.getEncerradoEm());
        }
        return vencidos.size();
    }

    private Cliente titular(UsuarioLogado usuario) {
        if (usuario.clienteId() == null) {
            // ADMIN criado pelo .env nao tem cadastro de cliente.
            throw new RegraNegocioException(CodigoErroEnum.SEM_CADASTRO_CLIENTE);
        }
        return clienteService.buscar(usuario.clienteId());
    }
}
