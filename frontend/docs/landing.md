# Landing RKF: cartão em 3D preso ao scroll

Roteiro (e prompt) da landing page em `app/page.tsx`. Adaptado de um modelo de banco PJ ("Lastro") para o RKF:
mesma estrutura cinematográfica, mas **só com o que o RKF faz de verdade** (contrato da API). O que ainda não
existe aparece como "em breve".

## Regras de conteúdo

- **Só prometer o que o produto faz hoje:** cadastro com CEP (ViaCEP), conta corrente ou poupança, depósito, saque,
  transferência, extrato, login com sessão de 15 min e idempotência nas movimentações. Cartão é **em breve**.
  Nada de câmbio, investimentos, Pix, gerente ou rendimento.
- Tom seco, com números e frases curtas. Nada de "revolucionar", "experiência", "exclusivo".
- Textos num lugar só: [`components/landing/conteudo.ts`](../components/landing/conteudo.ts). O saldo do app no
  hero (`SALDO_FINAL`) é o mesmo em que o extrato termina: a página conta uma história só.
- Rodapé avisa que é projeto de portfólio, sem autorização do Banco Central.

## Identidade

- Fundo marinho `#0a0f1c` com vinheta e grão fino; texto `#e8edf4`; suave `#8f9bb0`.
- **Um acento só:** ciano `#06b6d4`, em pontos pequenos (ponto, fio, marcador). Azul `#3b82f6` só em gradiente.
- Fontes: Plus Jakarta Sans (texto), Archivo expandida `wdth 125` (palavras gigantes), IBM Plex Mono (números e specs).
- **O cartão é o `BankCard`** (`components/marketing/bank-card.tsx`, o mesmo do `/cadastro`) em 3D: gradiente
  `#0f1e3d → #0a0f1c → #0b2a4a`, as 5 ondas azuis, "RKF", aproximação, chip dourado, `•••• 0731`, titular
  **FELLIPE SANTANA** e "DÉBITO" em ciano. Verso com tarja, assinatura, CVV mascarado e validade.

## Hero: o cartão (palco preso por 3,4 telas)

Camadas, de trás para a frente: "RKF" gigante (DOM, largura total) → celular em CSS (escondido embaixo) →
`<canvas>` transparente com o cartão em Three.js (a sombra cai nas letras) → textos.

1. **Início:** cartão em pé, girado 53°, na frente do "K", inclinando com o cursor (com inércia) e flutuando.
   Título "O banco que mostra / cada centavo.", três specs em mono e "Role 0%" com a porcentagem real.
2. **Vira:** os textos saem, o cartão cresce, se endireita e mostra o verso; "RKF" escurece.
   "Cartão RKF · em breve — Seu nome na frente. / O número só no verso."
3. **Encaixa:** o celular sobe **aparecendo aos poucos** (nunca opaco desde o início); o cartão gira para a frente
   e encaixa na vaga do app (`.app__vaga`, medida no DOM).
4. **App:** "Sua conta", saldo R$ 4.820,50, Depositar · Sacar · Transferir · Extrato e os 3 últimos lançamentos.
   Do lado: "01 — Conta / Saldo, extrato e transferência. / Numa tela só."

## As outras seções

| # | Seção | Ideia |
|---|---|---|
| 02 | Extrato que se escreve (pin curto) | lançamentos entram por cima empurrando os antigos; o saldo anda até o final; ponto ciano no mais novo |
| 03 | Segurança (pin curto) | "Clicou duas vezes? / Cobramos uma.": 3 envios, 1 débito (idempotência); specs de sessão, hash e limite de senha |
| 04 | Cadastro (respiro) | o CEP `01001-000` se digita com o scroll, o endereço aparece e uma linha percorre os 4 passos |
| 05 | Quem faz | Kevin, Renan e Fellipe em letras gigantes, subindo por máscara; relógio ao vivo de Osasco |
| 06 | Contas | três colunas só com fios: Corrente R$ 0, Poupança R$ 0, Cartão "Em breve" (ponto ciano) |
| — | Rodapé | "Abrir conta leva poucos minutos." + botão; "RKF" gigante cortado pela borda, subindo com o scroll |

**Nav:** fixa e transparente no topo; some ao descer e volta ao subir com fundo translúcido, blur e fio.
**Celular:** "RKF" na largura, cartão menor, textos empilhados; no fim, o celular grande cortado pela borda.
**Movimento reduzido:** sem rolagem suave, sem giro nem inclinação; o hero troca pela tela do app num corte
suave e o resto aparece no estado final.

## Como é construído

- Next.js 16 (App Router): a página é Server Component; cada seção com movimento é um Client Component.
- `three@0.186.1`, `gsap@3.15.0` (ScrollTrigger) e `lenis@1.3.26`, com versão exata no `package.json`.
- `lib/motion/cartao-3d.ts`: `criarCartao({ canvas, palco, reduzido, titular, final, fontes, estado })`.
  Câmera calibrada para 1 unidade = 1 px do palco; `estado = { cx, cy, len, rx, ry, rz, env, shadow, tilt, light }`
  é o que a timeline anima. `render()` roda no `gsap.ticker`; só desenha com o palco na tela.
- As poses do cartão são **funções** do tamanho do palco com `invalidateOnRefresh`: o encaixe continua exato
  em qualquer tela. A vaga é medida com o `transform` do celular desligado por um instante.
- **Ordem:** os ScrollTriggers com `pin` nascem nos efeitos das seções; o Lenis liga no efeito do pai
  (`LandingMovimento`), que no React roda depois dos filhos, e faz `lenis.resize()` a cada `refresh`.
- "RKF" (hero e rodapé) é ajustado à largura por JS (`ajustarPalavra`: mede a 100 px e escala).

## Armadilhas que já custaram tempo

- O canvas 2D precisa do nome real da fonte, e o `next/font` gera nomes com hash: leia a variável CSS
  (`--font-plus-jakarta-sans`, `--font-larga`, `--font-mono-landing`) e crie a textura depois de `document.fonts.ready`.
- `autoAlpha` dá `visibility: inherit` aos filhos: esconda **os filhos** (`.entra-depois > *`), nunca o contêiner.
- O Tailwind 4 posiciona com a propriedade `translate`, que o GSAP absorve ao animar `y`: anime os filhos, ou
  posicione com `left`/`top`.
- `PCFSoftShadowMap` saiu do Three.js: use `PCFShadowMap`.
