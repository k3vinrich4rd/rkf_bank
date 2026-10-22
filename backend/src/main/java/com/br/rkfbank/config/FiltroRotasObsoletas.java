package com.br.rkfbank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Avisa quem ainda chama uma rota antiga que ela vai sair, sem quebrar nada agora.
//
// Padrao de mercado para evoluir uma API: a rota nova entra, a antiga continua respondendo igual por um tempo e
// ganha dois headers padronizados. "Deprecation: true" (RFC 9745) diz que ela esta obsoleta; "Link: <...>;
// rel=\"successor-version\"" (RFC 8288) diz qual usar no lugar. O frontend e qualquer cliente conseguem detectar e
// migrar com calma; a remocao so acontece numa versao maior do contrato (3.0), combinada com quem consome a API.
@Component
public class FiltroRotasObsoletas extends OncePerRequestFilter {

    private record Obsoleta(String metodo, Pattern caminho, String sucessora) {
    }

    // Rota antiga -> rota que a substitui ($1 = o {contaId} da rota antiga).
    private static final List<Obsoleta> OBSOLETAS = List.of(
            new Obsoleta("POST", Pattern.compile("^/contas/clientes/[^/]+$"), "/contas"),
            new Obsoleta("POST", Pattern.compile("^/contas/([^/]+)/deposito$"), "/contas/$1/depositos"),
            new Obsoleta("POST", Pattern.compile("^/contas/([^/]+)/saque$"), "/contas/$1/saques"),
            new Obsoleta("POST", Pattern.compile("^/contas/transferencia$"), "/transferencias"),
            new Obsoleta("PATCH", Pattern.compile("^/auth/senha$"), "/usuarios/me/senha"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        for (Obsoleta obsoleta : OBSOLETAS) {
            Matcher casamento = obsoleta.caminho().matcher(request.getRequestURI());
            if (obsoleta.metodo().equals(request.getMethod()) && casamento.matches()) {
                response.setHeader("Deprecation", "true");
                response.setHeader("Link", "<" + casamento.replaceFirst(obsoleta.sucessora()) + ">; rel=\"successor-version\"");
                break;
            }
        }
        chain.doFilter(request, response);
    }
}
