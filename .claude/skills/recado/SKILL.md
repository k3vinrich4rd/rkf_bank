---
name: recado
description: Escreve, responde ou lista recados entre o time do RKF Bank (Kevin, Renan, Fellipe) em planing/recados.md, no modelo do arquivo. Use quando pedirem "/recado", "deixa um recado pro Renan", "responde o recado do Renan", "quais recados estão pendentes" ou similar.
---

# /recado

Arquivo: `planing/recados.md`. Modelo do time: `- [ ] De → Para (DD/MM): mensagem`.
Pessoas: **Kevin** (backend), **Renan** (frontend), **Fellipe** (idealizador, PO & QA e primeiro cliente). Quem escreve é o usuário Git atual (`git config user.name`; `k3vinrich4rd` = Kevin).

## Escrever um recado novo
1. Monte a mensagem curta e acionável: o que precisa, onde (`arquivo:linha` ou branch/PR) e até quando, se houver prazo.
2. Insira **no topo da lista**, logo depois da linha `---`, com a data de hoje.
3. Mostre a linha final antes de salvar se o pedido for vago.

## Responder um recado
1. Encontre o recado pelo assunto ou pela pessoa.
2. Marque `- [x]` e acrescente logo abaixo, indentado: `  - Resolvido: <resposta curta, com o PR ou commit>`.
3. Não apague recados: o histórico fica no arquivo.

## Listar pendentes
`Grep "^- \[ \]" planing/recados.md` e mostre uma tabela `de → para | data | assunto`, destacando os endereçados ao usuário atual.

## Quando usar automaticamente
Outras skills sugerem um recado quando:
- `/verificar-contrato` encontra divergência que o frontend precisa corrigir;
- `/nova-tela` precisa de um endpoint que o backend ainda não tem;
- uma mudança de contrato da API afeta o outro lado.

Não commite: o recado fica como alteração no `planing/recados.md` até o Kevin mandar (ele entra junto no `/entregar`). Sugestão de mensagem: `docs: adiciona recado para <pessoa>`.
