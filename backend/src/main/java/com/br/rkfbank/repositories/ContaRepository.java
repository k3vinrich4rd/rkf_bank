package com.br.rkfbank.repositories;

import com.br.rkfbank.dto.response.conta.ContaDivergenteResponseDto;
import com.br.rkfbank.entities.Conta;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Spring Data JPA: so a interface; o Spring gera a implementacao na subida. Metodos herdados do JpaRepository
// (findById, save, findAll...) cobrem o basico, e os declarados aqui sao "query methods": o SQL e montado a
// partir do NOME do metodo (existsByNumeroConta -> SELECT ... WHERE numero_conta = ? LIMIT 1).
public interface ContaRepository extends JpaRepository<Conta, UUID> {
    // Verifica unicidade do numero da conta.
    boolean existsByNumeroConta(String numeroConta);
    // Contas do cliente logado (GET /contas para o perfil CLIENTE).
    List<Conta> findByClienteId(UUID clienteId);
    Page<Conta> findByClienteId(UUID clienteId, Pageable pageable);

    // Conciliacao (GET /admin/conciliacao): contas cujo saldo difere de entradas - saidas do extrato. O saldo e gravado
    // a cada movimentacao junto com o lancamento (mesma transacao), entao os dois so divergem por bug ou mexida manual.
    @Query("""
            select new com.br.rkfbank.dto.response.conta.ContaDivergenteResponseDto(c.id, c.numeroConta, c.saldo,
                coalesce((select sum(e.valor) from Lancamento e where e.contaDestinoId = c.id), 0)
                - coalesce((select sum(s.valor) from Lancamento s where s.contaOrigemId = c.id), 0))
            from Conta c
            where c.saldo <> coalesce((select sum(e.valor) from Lancamento e where e.contaDestinoId = c.id), 0)
                - coalesce((select sum(s.valor) from Lancamento s where s.contaOrigemId = c.id), 0)
            order by c.numeroConta""")
    List<ContaDivergenteResponseDto> findDivergentesDoExtrato();
}
