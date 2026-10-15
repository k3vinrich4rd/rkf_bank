---
name: verificar-contrato
description: Compara as três fontes do contrato da API do RKF Bank - controllers e DTOs do backend, backend/README_CONTRATO_API.md e frontend/lib/api/types.ts + lib/api/*.ts - e lista divergências de rota, campo, tipo, obrigatoriedade e status HTTP. Use quando o usuário pedir "/verificar-contrato", ao mudar uma rota ou DTO, ou antes de integrar uma tela nova.
---

# /verificar-contrato

O contrato oficial é `backend/README_CONTRATO_API.md`. O backend implementa, o frontend espelha em `frontend/lib/api/types.ts`. Esta skill só **lê e relata**: não corrige nada sem o usuário pedir.

## Como verificar (barato)

1. Liste as rotas reais com uma busca, sem abrir arquivo por arquivo:
   `Grep "@(Get|Post|Put|Patch|Delete|Request)Mapping" backend/src/main/java/com/br/rkfbank/controllers`
2. Leia os `record` de `backend/src/main/java/com/br/rkfbank/dto/**` (são curtos) e as anotações de validação.
3. Leia as seções 2 a 6 do contrato (tabela de endpoints, requests, responses, erros).
4. Leia `frontend/lib/api/types.ts` e os caminhos usados em `frontend/lib/api/clientes.ts` e `contas.ts`.
5. Procure `TODO(backend)` no frontend: são pedidos do Renan para o backend.

## O que comparar

| Item | Backend × Contrato | Contrato × Frontend |
|---|---|---|
| Método + caminho de cada rota | ✔ | ✔ |
| Nome e tipo de cada campo do request e do response | ✔ | ✔ |
| Campo obrigatório × opcional (`@NotBlank`/`@NotNull` × `?` no TS) | ✔ | ✔ |
| Enums (`TipoContaEnum`, `TipoLancamentoEnum`) | ✔ | ✔ |
| Status de sucesso e de erro de cada rota | ✔ | (tratamento em `ApiError.status`) |
| Formato do erro padrão (`campos[].campo/mensagem`) | ✔ | ✔ |

## Saída

Uma tabela por gravidade:
- 🔴 **quebra em produção** (rota ou campo que um lado envia e o outro não aceita);
- 🟡 **contrato desatualizado** (documento diz uma coisa, código faz outra);
- 🟢 **sugestão** (ex.: endpoint que o frontend contorna, como o `TODO(backend)` de `GET /contas/{contaId}`).

Para cada item: onde está (`arquivo:linha`), o que diverge e quem precisa mudar (backend, contrato ou frontend). Lembre que o frontend é do Renan: para itens dele, sugira o texto de um recado em `planing/recados.md`.
