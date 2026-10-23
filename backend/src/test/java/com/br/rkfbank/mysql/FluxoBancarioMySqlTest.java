package com.br.rkfbank.mysql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

// O roteiro E2E principal (pasta 02 da colecao do Insomnia) pelas rotas, gravando no MySQL 8.4 real:
// cadastro com mascara -> login -> contas ORIGEM e DESTINO -> deposito, saque e transferencia -> saldos e extrato.
class FluxoBancarioMySqlTest extends MySqlIntegracaoBase {

    @Test
    void doCadastroAoExtratoNoMySql() throws Exception {
        String cpf = cpfValidoAleatorio();
        String cpfComMascara = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomeCompleto":"Cliente Novo do Insomnia","cpf":"%s","email":"novo.%s@exemplo.test",
                         "telefone":"(11) 99999-8888","cep":"06020-010","numero":"123","complemento":"Apto 45","senha":"%s","aceiteTermos":true}"""
                        .formatted(cpfComMascara, cpf, SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**"))
                .andExpect(jsonPath("$.termosVersao").value("2026-10-23"))
                .andExpect(jsonPath("$.telefone").value("(11) 99999-8888"))
                .andExpect(jsonPath("$.endereco.cidade").value("Osasco"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        String token = login(cpf);
        String origem = abrirConta(token, "CORRENTE");
        String destino = abrirConta(token, "POUPANCA");

        depositar(token, origem, "150.00");
        mockMvc.perform(post("/contas/" + origem + "/saques").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":50.00}"))
                .andExpect(status().isNoContent());

        String chave = UUID.randomUUID().toString();
        String transferencia = "{\"contaOrigemId\":\"" + origem + "\",\"contaDestinoId\":\"" + destino + "\",\"valor\":25.00}";
        mockMvc.perform(post("/transferencias").header("Authorization", "Bearer " + token).header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON).content(transferencia))
                .andExpect(status().isNoContent());
        // A repeticao com a mesma chave nao move dinheiro de novo (a chave e UNIQUE no MySQL tambem).
        mockMvc.perform(post("/transferencias").header("Authorization", "Bearer " + token).header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON).content(transferencia))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        mockMvc.perform(get("/contas/" + origem).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(75.00))
                .andExpect(jsonPath("$.tipoConta").value("CORRENTE"));
        mockMvc.perform(get("/contas/" + destino).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.saldo").value(25.00));

        mockMvc.perform(get("/contas/" + origem + "/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tipoLancamento").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$[1].tipoLancamento").value("SAQUE"))
                .andExpect(jsonPath("$[2].tipoLancamento").value("DEPOSITO"));
    }

    @Test
    void saldoInsuficienteNaoGravaNadaNoMySql() throws Exception {
        String token = clienteNovoComToken();
        String conta = abrirConta(token, "CORRENTE");
        depositar(token, conta, "10.00");

        mockMvc.perform(post("/contas/" + conta + "/saques").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10.01}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("SALDO_INSUFICIENTE"));

        mockMvc.perform(get("/contas/" + conta + "/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
