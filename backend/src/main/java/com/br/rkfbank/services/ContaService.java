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
import com.br.rkfbank.exceptions.ContaNaoEncontradaException;
import com.br.rkfbank.exceptions.NaoProcessavelException;
import com.br.rkfbank.exceptions.NegocioException;
import com.br.rkfbank.repositories.ContaRepository;
import com.br.rkfbank.repositories.LancamentoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {

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
    public ContaResponseDto abrir(UUID clienteId, AbrirContaRequestDto request) {
        // Garante que a conta sempre nasce vinculada a um cliente existente.
        Cliente cliente = clienteService.buscar(clienteId);

        // Regra de unicidade de número da conta.
        if (contaRepository.existsByNumeroConta(request.numeroConta())) {
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
        try {
            return paraContaResponse(contaRepository.saveAndFlush(conta));
        } catch (DataIntegrityViolationException ex) {
            throw new NegocioException("Numero de conta ja existe");
        }
    }

    @Transactional
    public void depositar(UUID contaId, DepositarRequestDto request) {
        // Só movimenta conta existente e ativa.
        Conta conta = buscarAtiva(contaId);
        // Valor monetário validado e normalizado em 2 casas.
        BigDecimal valor = validarValor(request.valor());

        conta.setSaldo(conta.getSaldo().add(valor));
        contaRepository.save(conta);

        // Extrato: depósito é lançamento de entrada com origem nula.
        salvarLancamento(TipoLancamentoEnum.DEPOSITO, valor, null, conta.getId(), request.descricao());
    }

    @Transactional
    public void sacar(UUID contaId, SaqueRequestDto request) {
        Conta conta = buscarAtiva(contaId);
        BigDecimal valor = validarValor(request.valor());

        // Regra de saldo antes de debitar.
        if (conta.getSaldo().compareTo(valor) < 0) {
            throw new NaoProcessavelException("Saldo insuficiente");
        }

        conta.setSaldo(conta.getSaldo().subtract(valor));
        contaRepository.save(conta);

        // Extrato: saque é lançamento de saída com destino nulo.
        salvarLancamento(TipoLancamentoEnum.SAQUE, valor, conta.getId(), null, request.descricao());
    }

    @Transactional
    public void transferir(TransferirRequestDto request) {
        // Regra para impedir transferência para a mesma conta.
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            throw new NaoProcessavelException("Conta de origem e destino nao podem ser iguais");
        }

        BigDecimal valor = validarValor(request.valor());
        Conta origem = buscarAtiva(request.contaOrigemId());
        Conta destino = buscarAtiva(request.contaDestinoId());

        // Regra de saldo da origem antes de aplicar débito/crédito.
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new NaoProcessavelException("Saldo insuficiente para transferencia");
        }

        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));

        // Mesma transação para manter atomicidade financeira.
        contaRepository.save(origem);
        contaRepository.save(destino);

        // Extrato: transferência registra origem e destino no mesmo lançamento.
        salvarLancamento(TipoLancamentoEnum.TRANSFERENCIA, valor, origem.getId(), destino.getId(), request.descricao());
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponseDto> listarLancamentos(UUID contaId) {
        // Valida conta antes de consultar extrato.
        buscarAtiva(contaId);

        return lancamentoRepository
                .findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId)
                .stream()
                .map(this::paraLancamentoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContaResponseDto> listarTodas() {
        return contaRepository.findAll().stream().map(this::paraContaResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ContaResponseDto> listarPaginado(Pageable pageable) {
        return contaRepository.findAll(pageable).map(this::paraContaResponse);
    }

    private Conta buscarAtiva(UUID contaId) {
        // 404 quando a conta não existe.
        Conta conta = contaRepository.findById(contaId).orElseThrow(ContaNaoEncontradaException::new);
        // 422 quando existe, mas está inativa.
        if (!conta.isAtiva()) {
            throw new NaoProcessavelException("Conta inativa");
        }
        return conta;
    }

    private BigDecimal validarValor(BigDecimal valor) {
        // Garante valor positivo para qualquer movimentação financeira.
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
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