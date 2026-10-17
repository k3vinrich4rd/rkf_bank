package com.br.rkfbank.services;

import com.br.rkfbank.dto.request.conta.AbrirContaRequestDto;
import com.br.rkfbank.dto.request.conta.DepositarRequestDto;
import com.br.rkfbank.dto.request.conta.SaqueRequestDto;
import com.br.rkfbank.dto.request.conta.TransferirRequestDto;
import com.br.rkfbank.dto.response.conta.ContaResponseDto;
import com.br.rkfbank.dto.response.conta.LancamentoResponseDto;
import com.br.rkfbank.entities.Cliente;
import com.br.rkfbank.entities.Conta;
import com.br.rkfbank.entities.Lancamento;
import com.br.rkfbank.entities.enums.TipoLancamentoEnum;
import com.br.rkfbank.exceptions.AcessoNegadoException;
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.LancamentoRepository;
import com.br.rkfbank.security.UsuarioLogado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {

    private static final Logger log = LogManager.getLogger(ContaService.class);

    private final ContaRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ClienteService clienteService;

    public ContaService(
            ContaRepository contaRepository,
            LancamentoRepository lancamentoRepository,
            ClienteService clienteService
    ) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.clienteService = clienteService;
    }

    @Transactional
    public ContaResponseDto abrir(UUID clienteId, AbrirContaRequestDto request, UsuarioLogado usuario) {
        // Regra de dono: so abre conta no proprio cadastro (o clienteId da URL tem que ser o do token).
        if (!usuario.ehDonoDoCliente(clienteId)) {
            log.warn("Abertura de conta recusada: usuario {} tentou abrir conta para o cliente {}", usuario.usuarioId(), clienteId);
            throw new AcessoNegadoException("Voce so pode abrir conta no seu proprio cadastro");
        }

        // Garante que a conta sempre nasce vinculada a um cliente existente.
        Cliente cliente = clienteService.buscar(clienteId);

        // Regra de unicidade de número da conta.
        if (contaRepository.existsByNumeroConta(request.numeroConta())) {
            log.warn("Abertura de conta recusada para cliente {}: numero {} ja existe", clienteId, request.numeroConta());
            throw new NegocioException("Numero de conta ja existe");
        }

        Conta conta = new Conta();
        conta.setCliente(cliente);
        conta.setAgencia(request.agencia());
        conta.setNumeroConta(request.numeroConta());
        conta.setTipoConta(request.tipoContaEnum());
        conta.setSaldo(new BigDecimal("0.00"));
        conta.setAtiva(true);

        // Retorna DTO para não expor entidade JPA diretamente na API.
        Conta salva;
        try {
            salva = contaRepository.saveAndFlush(conta);
        } catch (DataIntegrityViolationException ex) {
            // Abertura concorrente com o mesmo numero passou pela checagem acima e esbarrou na constraint do banco.
            log.warn("Abertura de conta recusada para cliente {}: numero {} ja existe (constraint do banco)",
                    clienteId, request.numeroConta());
            throw new NegocioException("Numero de conta ja existe");
        }
        log.info("Conta {} ({}) aberta para cliente {}", salva.getId(), salva.getTipoConta(), clienteId);
        return paraContaResponse(salva);
    }

    @Transactional
    public void depositar(UUID contaId, DepositarRequestDto request, UsuarioLogado usuario) {
        // Só movimenta conta existente, do usuário logado e ativa.
        Conta conta = buscarDoDono(contaId, usuario);
        // Valor monetário validado e normalizado em 2 casas.
        BigDecimal valor = validarValor(request.valor());

        conta.setSaldo(conta.getSaldo().add(valor));
        contaRepository.save(conta);

        // Extrato: depósito é lançamento de entrada com origem nula.
        salvarLancamento(TipoLancamentoEnum.DEPOSITO, valor, null, conta.getId(), request.descricao());
        log.info("Deposito de {} na conta {}", valor, contaId);
    }

    @Transactional
    public void sacar(UUID contaId, SaqueRequestDto request, UsuarioLogado usuario) {
        Conta conta = buscarDoDono(contaId, usuario);
        BigDecimal valor = validarValor(request.valor());

        // Regra de saldo antes de debitar.
        if (conta.getSaldo().compareTo(valor) < 0) {
            log.warn("Saque de {} recusado na conta {}: saldo insuficiente", valor, contaId);
            throw new NaoProcessavelException("Saldo insuficiente");
        }

        conta.setSaldo(conta.getSaldo().subtract(valor));
        contaRepository.save(conta);

        // Extrato: saque é lançamento de saída com destino nulo.
        salvarLancamento(TipoLancamentoEnum.SAQUE, valor, conta.getId(), null, request.descricao());
        log.info("Saque de {} na conta {}", valor, contaId);
    }

    @Transactional
    public void transferir(TransferirRequestDto request, UsuarioLogado usuario) {
        // Regra para impedir transferência para a mesma conta.
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            log.warn("Transferencia recusada: origem e destino iguais ({})", request.contaOrigemId());
            throw new NaoProcessavelException("Conta de origem e destino nao podem ser iguais");
        }

        BigDecimal valor = validarValor(request.valor());
        // So a origem precisa ser do usuario logado; o destino pode ser de qualquer cliente.
        Conta origem = buscarDoDono(request.contaOrigemId(), usuario);
        Conta destino = buscarAtiva(request.contaDestinoId());

        // Regra de saldo da origem antes de aplicar débito/crédito.
        if (origem.getSaldo().compareTo(valor) < 0) {
            log.warn("Transferencia de {} recusada da conta {} para {}: saldo insuficiente",
                    valor, origem.getId(), destino.getId());
            throw new NaoProcessavelException("Saldo insuficiente para transferencia");
        }

        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));

        // Mesma transação para manter atomicidade financeira.
        contaRepository.save(origem);
        contaRepository.save(destino);

        // Extrato: transferência registra origem e destino no mesmo lançamento.
        salvarLancamento(TipoLancamentoEnum.TRANSFERENCIA, valor, origem.getId(), destino.getId(), request.descricao());
        log.info("Transferencia de {} da conta {} para {}", valor, origem.getId(), destino.getId());
    }

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

    // Ordem 404 -> 403 -> 422: o dono e conferido antes de qualquer regra que revele o estado da conta.
    private Conta buscarDoDono(UUID contaId, UsuarioLogado usuario) {
        Conta conta = buscar(contaId);
        // 403 quando a conta existe, mas é de outro cliente (o clienteId vem do token assinado, não do request).
        if (!usuario.ehDonoDoCliente(conta.getCliente().getId())) {
            log.warn("Operacao recusada: usuario {} nao e dono da conta {}", usuario.usuarioId(), contaId);
            throw new AcessoNegadoException("Conta nao pertence ao usuario logado");
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
            throw new NaoProcessavelException("Conta inativa");
        }
        return conta;
    }

    private BigDecimal validarValor(BigDecimal valor) {
        // Garante valor positivo para qualquer movimentação financeira.
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Operacao recusada: valor {} nao e positivo", valor);
            throw new NaoProcessavelException("Valor deve ser maior que zero");
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