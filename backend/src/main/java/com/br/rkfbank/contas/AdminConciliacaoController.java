package com.br.rkfbank.contas;

import com.br.rkfbank.contas.dto.response.ContaDivergenteResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// So ADMIN (ConfiguracaoSeguranca: /admin/** -> hasRole("ADMIN")). Antes so rodava nos testes; agora da para conferir
// em producao, a qualquer momento, se algum saldo deixou de bater com o extrato.
@Tag(name = "Administração", description = "gestão de usuários e conciliação (só ADMIN)")
@RestController
public class AdminConciliacaoController {

    private final ContaService contaService;

    public AdminConciliacaoController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/admin/conciliacao")
    public ResponseEntity<List<ContaDivergenteResponseDto>> conciliar() {
        return ResponseEntity.ok(contaService.conciliar());
    }
}
