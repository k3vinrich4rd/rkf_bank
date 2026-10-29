package com.br.rkfbank.contas;

import com.br.rkfbank.autenticacao.UsuarioLogado;
import com.br.rkfbank.clientes.Cliente;
import com.br.rkfbank.clientes.ClienteService;
import com.br.rkfbank.compartilhado.erros.AcessoNegadoException;
import com.br.rkfbank.compartilhado.erros.CodigoErroEnum;
import com.br.rkfbank.compartilhado.erros.ConflitoException;
import com.br.rkfbank.compartilhado.erros.ContaNaoEncontradaException;
import com.br.rkfbank.compartilhado.erros.RegraNegocioException;
import com.br.rkfbank.contas.comprovante.ComprovanteService;
import com.br.rkfbank.contas.dto.request.AberturaContaRequestDto;
import com.br.rkfbank.contas.dto.request.DepositoRequestDto;
import com.br.rkfbank.contas.dto.request.SaqueRequestDto;
import com.br.rkfbank.contas.dto.request.TransferenciaRequestDto;
import com.br.rkfbank.contas.dto.response.ContaDivergenteResponseDto;
import com.br.rkfbank.contas.dto.response.ContaResponseDto;
import com.br.rkfbank.contas.dto.response.LancamentoResponseDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Regras de negocio das contas: abrir, depositar, sacar, transferir e extrato.
// Toda operacao comeca descobrindo QUEM pede (UsuarioLogado, vindo do token) e checando se a conta e dele
// (regra de dono -> 403). Toda escrita e @Transactional: saldo e lancamento sao gravados juntos ou nenhum.
// Nao conhece HTTP: recebe DTOs e o usuario, devolve DTOs ou lanca excecoes que o handler traduz em status.
@Service
public class ContaService {

    private static final Logger log = LogManager.getLogger(ContaService.class);

    static final String AGENCIA_PADRAO = "0001";
    private static final int TENTATIVAS_NUMERO_LIVRE = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ClienteService clienteService;
    private final ComprovanteService comprovanteService;

    public ContaService(
            ContaRepository contaRepository,
            LancamentoRepository lancamentoRepository,
            ClienteService clienteService,
            ComprovanteService comprovanteService
    ) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.clienteService = clienteService;
        this.comprovanteService = comprovanteService;
    }

    // Abre conta no cadastro de quem esta logado: o cliente vem do token, entao nao ha "cadastro de outro" para
    // conferir. Agencia 0001 e numero gerado pelo banco (com digito verificador): numero de conta e identidade do
    // banco, nao escolha de quem abre.
    @Transactional
    public ContaResponseDto abrir(AberturaContaRequestDto request, UsuarioLogado usuario) {
        // ADMIN criado pelo .env nao tem cadastro de cliente: nao ha onde pendurar a conta.
        UUID clienteId = usuario.clienteId();
        if (clienteId == null) {
            log.warn("Abertura de conta recusada: usuario {} sem cadastro de cliente", usuario.usuarioId());
            throw new RegraNegocioException(CodigoErroEnum.SEM_CADASTRO_CLIENTE);
        }

        // Garante que a conta sempre nasce vinculada a um cliente existente.
        Cliente cliente = clienteService.buscar(clienteId);
        String numeroConta = gerarNumeroLivre();

        Conta conta = new Conta();
        conta.setCliente(cliente);
        conta.setAgencia(AGENCIA_PADRAO);
        conta.setNumeroConta(numeroConta);
        conta.setTipoConta(request.tipoConta());
        conta.setSaldo(new BigDecimal("0.00"));
        conta.setAtiva(true);

        // Retorna DTO para não expor entidade JPA diretamente na API.
        Conta salva;
        try {
            salva = contaRepository.saveAndFlush(conta);
        } catch (DataIntegrityViolationException ex) {
            // Abertura concorrente com o mesmo numero passou pela checagem acima e esbarrou na constraint do banco.
            log.warn("Abertura de conta recusada para cliente {}: numero {} ja existe (constraint do banco)",
                    clienteId, numeroConta);
            throw new ConflitoException(CodigoErroEnum.NUMERO_CONTA_INDISPONIVEL);
        }
        log.info("Conta {} ({}) aberta para cliente {}", salva.getId(), salva.getTipoConta(), clienteId);
        return paraContaResponse(salva);
    }

    // Deposito: so na propria conta e ativa. Soma no saldo e grava um lancamento DEPOSITO (so com destino).
    @Transactional
    public void depositar(UUID contaId, DepositoRequestDto request, UsuarioLogado usuario) {
        // Só movimenta conta existente, do usuário logado e ativa.
        Conta conta = buscarDoDono(contaId, usuario);
        // Valor monetário validado e normalizado em 2 casas.
        BigDecimal valor = validarValor(request.valor());

        conta.depositar(valor);
        contaRepository.save(conta);

        // Extrato: depósito é lançamento de entrada com origem nula.
        salvarLancamento(TipoLancamentoEnum.DEPOSITO, valor, null, conta.getId(), request.descricao());
        log.info("Deposito de {} na conta {}", valor, contaId);
    }

    // Saque: a regra de saldo (422 se faltar) mora na Conta.sacar, testada no ContaTest.
    @Transactional
    public void sacar(UUID contaId, SaqueRequestDto request, UsuarioLogado usuario) {
        Conta conta = buscarDoDono(contaId, usuario);
        BigDecimal valor = validarValor(request.valor());

        conta.sacar(valor); // SALDO_INSUFICIENTE sobe daqui; o TratadorGlobalDeErros loga e devolve 422
        contaRepository.save(conta);

        // Extrato: saque é lançamento de saída com destino nulo.
        salvarLancamento(TipoLancamentoEnum.SAQUE, valor, conta.getId(), null, request.descricao());
        log.info("Saque de {} na conta {}", valor, contaId);
    }

    // Transferencia: debito na origem, credito no destino e UM lancamento com as duas contas, na mesma transacao.
    // Se qualquer passo falhar (inclusive o lock otimista no commit), nada acontece: dinheiro nao some nem aparece.
    @Transactional
    public void transferir(TransferenciaRequestDto request, UsuarioLogado usuario) {
        // Regra para impedir transferência para a mesma conta.
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            log.warn("Transferencia recusada: origem e destino iguais ({})", request.contaOrigemId());
            throw new RegraNegocioException(CodigoErroEnum.MESMA_CONTA);
        }

        BigDecimal valor = validarValor(request.valor());
        // So a origem precisa ser do usuario logado; o destino pode ser de qualquer cliente.
        Conta origem = buscarDoDono(request.contaOrigemId(), usuario);
        Conta destino = buscarAtiva(request.contaDestinoId());

        // Débito e crédito tudo-ou-nada na entidade (saldo conferido antes de mexer em qualquer conta).
        origem.transferirPara(destino, valor);

        // Mesma transação para manter atomicidade financeira.
        contaRepository.save(origem);
        contaRepository.save(destino);

        // Extrato: transferência registra origem e destino no mesmo lançamento.
        salvarLancamento(TipoLancamentoEnum.TRANSFERENCIA, valor, origem.getId(), destino.getId(), request.descricao());
        log.info("Transferencia de {} da conta {} para {}", valor, origem.getId(), destino.getId());
    }

    // Extrato: tudo que entrou ou saiu da conta, do mais novo para o mais antigo. So o dono ve.
    @Transactional(readOnly = true)
    public List<LancamentoResponseDto> listarLancamentos(UUID contaId, UsuarioLogado usuario) {
        // Valida conta (e o dono) antes de consultar extrato.
        buscarDoDono(contaId, usuario);

        List<LancamentoResponseDto> lancamentos = lancamentoRepository
                .findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId)
                .stream()
                .map(this::paraLancamentoResponse)
                .toList();
        log.debug("Extrato da conta {} retornou {} lancamentos", contaId, lancamentos.size());
        return lancamentos;
    }

    // GET /contas/{contaId}: uma conta so (saldo atualizado depois de uma movimentacao). Mesma regra do GET /contas:
    // o CLIENTE ve so as dele (403 nas dos outros) e o ADMIN ve qualquer uma. Conta inativa tambem aparece (ativa=false).
    @Transactional(readOnly = true)
    public ContaResponseDto buscarPorId(UUID contaId, UsuarioLogado usuario) {
        Conta conta = buscar(contaId);
        if (!usuario.isAdmin() && !usuario.ehDono(conta.getCliente().getId())) {
            log.warn("Consulta recusada: usuario {} nao e dono da conta {}", usuario.usuarioId(), contaId);
            throw new AcessoNegadoException();
        }
        return paraContaResponse(conta);
    }

    // Conciliacao saldo x extrato (so ADMIN). Lista vazia = tudo certo. Divergencia vira WARN no log para alertar.
    @Transactional(readOnly = true)
    public List<ContaDivergenteResponseDto> conciliar() {
        List<ContaDivergenteResponseDto> divergentes = contaRepository.findDivergentesDoExtrato();
        if (!divergentes.isEmpty()) {
            log.warn("Conciliacao: {} conta(s) com saldo diferente do extrato", divergentes.size());
        }
        return divergentes;
    }

    // GET /contas: o ADMIN ve todas; o CLIENTE ve so as dele. A mesma rota muda o resultado conforme o perfil.
    @Transactional(readOnly = true)
    public List<ContaResponseDto> listarTodas(UsuarioLogado usuario) {
        // ADMIN ve todas; CLIENTE so as dele.
        List<Conta> encontradas = usuario.isAdmin()
                ? contaRepository.findAll()
                : contaRepository.findByClienteId(usuario.clienteId());
        List<ContaResponseDto> contas = encontradas.stream().map(this::paraContaResponse).toList();
        log.debug("Listagem de contas retornou {} registros", contas.size());
        return contas;
    }

    // GET /clientes/{clienteId}/contas: so ADMIN (suporte, conciliacao). A rota ja e barrada na ConfiguracaoSeguranca;
    // a checagem aqui e a segunda camada (defesa em profundidade): se alguem mudar a regra da rota por engano, o
    // CLIENTE continua sem conseguir listar contas de outra pessoa pelo id da URL. Cliente comum usa GET /contas.
    // Ordem 403 -> 404: quem nao e ADMIN nao descobre se o id de cliente existe.
    @Transactional(readOnly = true)
    public List<ContaResponseDto> listarDoCliente(UUID clienteId, UsuarioLogado usuario) {
        return contaRepository.findByClienteId(clienteParaAdmin(clienteId, usuario)).stream()
                .map(this::paraContaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ContaResponseDto> listarDoClientePaginado(UUID clienteId, Pageable pageable, UsuarioLogado usuario) {
        return contaRepository.findByClienteId(clienteParaAdmin(clienteId, usuario), pageable).map(this::paraContaResponse);
    }

    private UUID clienteParaAdmin(UUID clienteId, UsuarioLogado usuario) {
        if (!usuario.isAdmin()) {
            log.warn("Listagem de contas por cliente recusada: usuario {} nao e ADMIN", usuario.usuarioId());
            throw new AcessoNegadoException();
        }
        return clienteService.buscar(clienteId).getId(); // 404 se o cliente nao existe
    }

    // Versao paginada: o banco devolve so uma fatia (LIMIT/OFFSET) e o total, em vez da tabela inteira.
    @Transactional(readOnly = true)
    public Page<ContaResponseDto> listarPaginado(Pageable pageable, UsuarioLogado usuario) {
        Page<Conta> encontradas = usuario.isAdmin()
                ? contaRepository.findAll(pageable)
                : contaRepository.findByClienteId(usuario.clienteId(), pageable);
        Page<ContaResponseDto> pagina = encontradas.map(this::paraContaResponse);
        log.debug("Listagem paginada de contas: pagina {} com {} de {} registros",
                pageable.getPageNumber(), pagina.getNumberOfElements(), pagina.getTotalElements());
        return pagina;
    }

    // 7 digitos aleatorios + 1 digito verificador (modulo 11, como nos bancos): 8 digitos que ninguem escolhe nem adivinha
    // em sequencia. SecureRandom para o proximo numero nao ser previsivel. Colisao e rara; ainda assim, tenta de novo.
    private String gerarNumeroLivre() {
        for (int tentativa = 1; tentativa <= TENTATIVAS_NUMERO_LIVRE; tentativa++) {
            String numero = gerarNumeroConta(ALEATORIO);
            if (!contaRepository.existsByNumeroConta(numero)) {
                return numero;
            }
            log.debug("Numero de conta gerado ja existia (tentativa {})", tentativa);
        }
        throw new ConflitoException(CodigoErroEnum.NUMERO_CONTA_INDISPONIVEL);
    }

    static String gerarNumeroConta(SecureRandom aleatorio) {
        String base = String.valueOf(1_000_000 + aleatorio.nextInt(9_000_000));
        return base + digitoVerificador(base);
    }

    // Pesos 2,3,4... da direita para a esquerda; resto 0 ou 1 vira digito 0.
    static int digitoVerificador(String base) {
        int soma = 0;
        int peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(base.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // Ordem 404 -> 403 -> 422: o dono e conferido antes de qualquer regra que revele o estado da conta.
    private Conta buscarDoDono(UUID contaId, UsuarioLogado usuario) {
        Conta conta = buscar(contaId);
        // 403 quando a conta existe, mas é de outro cliente (o clienteId vem do token assinado, não do request).
        if (!usuario.ehDono(conta.getCliente().getId())) {
            log.warn("Operacao recusada: usuario {} nao e dono da conta {}", usuario.usuarioId(), contaId);
            throw new AcessoNegadoException();
        }
        return garantirAtiva(conta);
    }

    private Conta buscarAtiva(UUID contaId) {
        return garantirAtiva(buscar(contaId));
    }

    private Conta buscar(UUID contaId) {
        // 404 quando a conta não existe.
        return contaRepository.findById(contaId).orElseThrow(() -> {
            log.warn("Conta {} nao encontrada", contaId);
            return new ContaNaoEncontradaException();
        });
    }

    private Conta garantirAtiva(Conta conta) {
        UUID contaId = conta.getId();
        // 422 quando existe, mas está inativa.
        if (!conta.isAtiva()) {
            log.warn("Operacao recusada: conta {} inativa", contaId);
            throw new RegraNegocioException(CodigoErroEnum.CONTA_INATIVA);
        }
        return conta;
    }

    private BigDecimal validarValor(BigDecimal valor) {
        // Garante valor positivo para qualquer movimentação financeira.
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Operacao recusada: valor {} nao e positivo", valor);
            throw new RegraNegocioException(CodigoErroEnum.VALOR_INVALIDO);
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private void salvarLancamento(TipoLancamentoEnum tipo, BigDecimal valor, UUID origem, UUID destino, String descricao) {
        Lancamento lancamento = new Lancamento();
        lancamento.setTipoLancamentoEnum(tipo);
        lancamento.setValor(valor);
        lancamento.setContaOrigemId(origem);
        lancamento.setContaDestinoId(destino);
        lancamento.setDescricao(descricao);
        lancamentoRepository.save(lancamento);
        // Outbox: o comprovante entra na fila NA MESMA transacao do dinheiro e sai depois (PublicadorOutbox).
        comprovanteService.registrar(lancamento);
    }

    private ContaResponseDto paraContaResponse(Conta conta) {
        return new ContaResponseDto(
                conta.getId(),
                conta.getCliente().getId(),
                conta.getAgencia(),
                conta.getNumeroConta(),
                conta.getTipoConta(),
                conta.getSaldo(),
                conta.isAtiva()
        );
    }

    private LancamentoResponseDto paraLancamentoResponse(Lancamento lancamento) {
        return new LancamentoResponseDto(
                lancamento.getId(),
                lancamento.getTipoLancamentoEnum(),
                lancamento.getValor(),
                lancamento.getContaOrigemId(),
                lancamento.getContaDestinoId(),
                lancamento.getDescricao(),
                lancamento.getDataHora()
        );
    }
}