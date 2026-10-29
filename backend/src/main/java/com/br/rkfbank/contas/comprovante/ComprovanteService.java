package com.br.rkfbank.contas.comprovante;

import com.br.rkfbank.contas.Lancamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

// Lado da escrita do Outbox: o ContaService chama logo depois de gravar o lancamento.
// MANDATORY: so roda DENTRO de uma transacao que ja existe (a do dinheiro). Sem ela, estoura em vez de gravar o
// evento sozinho. E isso que garante "evento existe se, e somente se, o dinheiro se moveu".
@Service
public class ComprovanteService {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public ComprovanteService(OutboxRepository outboxRepository, JsonMapper jsonMapper) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Lancamento lancamento) {
        String payload = jsonMapper.writeValueAsString(MovimentacaoRealizada.de(lancamento));
        outboxRepository.save(new EventoOutbox(MovimentacaoRealizada.TIPO_EVENTO, payload));
    }
}
